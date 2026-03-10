#!/usr/bin/env python3
"""Extract the APK v2 signing certificate hash from an APK file.

This script replicates the exact byte-parsing sequence used by the KamiSU
kernel module (kernel/apk_sign.c: check_v2_signature / check_block) so that
the extracted values can be placed directly into kernel/kamisu_sign.h.

Usage:
    python3 extract_apk_hash.py <apk_file>

Output (stdout, one per line):
    CERT_SIZE=<decimal integer>
    CERT_HASH=<64-char lowercase SHA-256 hex string>

Exit code is non-zero on any error.
"""

import hashlib
import os
import struct
import sys

# APK signing-block constants
_APK_SIG_BLOCK_MAGIC = b"APK Sig Block 42"
_APK_SIG_SCHEME_V2_ID = 0x7109871A
_EOCD_MAGIC = 0x06054B50
_CERT_MAX_LENGTH = 1024


# ---------------------------------------------------------------------------
# EOCD / central-directory helpers
# ---------------------------------------------------------------------------

def _find_cd_offset(data: bytes) -> int:
    """Return the central-directory offset stored in the EOCD record.

    Searches backwards from the end of *data* for a valid End-of-Central-
    Directory record (same algorithm as the kernel's check_v2_signature loop).
    Raises ValueError if no valid EOCD is found.
    """
    size = len(data)
    for i in range(0, 65536):
        # The kernel reads 2 bytes from position (file_size - i - 2).
        # Those 2 bytes are the EOCD comment-length field when n == i.
        pos = size - i - 2
        if pos < 0:
            break
        n = struct.unpack_from("<H", data, pos)[0]
        if n != i:
            continue
        # Possible EOCD start = pos - 20
        eocd = pos - 20
        if eocd < 0:
            continue
        magic = struct.unpack_from("<I", data, eocd)[0]
        if magic == _EOCD_MAGIC:
            return struct.unpack_from("<I", data, eocd + 16)[0]
    raise ValueError("EOCD record not found – is this a valid APK/ZIP?")


# ---------------------------------------------------------------------------
# APK signing-block helpers
# ---------------------------------------------------------------------------

def _find_signing_block(data: bytes, cd_offset: int):
    """Locate the APK signing block just before the central directory.

    Returns (block_bytes, size_of_block) where block_bytes starts at the
    leading uint64 size field (same as the kernel's pos = size4 - (size8+8)).
    Raises ValueError if the block is absent or malformed.
    """
    if cd_offset < 24:
        raise ValueError("File too small to contain an APK signing block")

    # The footer of the signing block is:
    #   [cd_offset-24 .. cd_offset-16]  uint64 size_of_block (trailing copy)
    #   [cd_offset-16 .. cd_offset]     "APK Sig Block 42"
    magic = data[cd_offset - 16 : cd_offset]
    if magic != _APK_SIG_BLOCK_MAGIC:
        raise ValueError("APK Sig Block 42 magic not found before central directory")

    size_of_block = struct.unpack_from("<Q", data, cd_offset - 24)[0]
    block_start = cd_offset - size_of_block - 8
    if block_start < 0:
        raise ValueError(f"Signing block size {size_of_block} is too large")

    # Verify the leading size matches the trailing size (sanity check)
    leading_size = struct.unpack_from("<Q", data, block_start)[0]
    if leading_size != size_of_block:
        raise ValueError("Signing block leading/trailing size mismatch")

    return data[block_start : cd_offset], size_of_block


def _find_v2_block_value(block: bytes, size_of_block: int) -> bytes:
    """Scan the signing block key-value pairs for the v2 scheme entry.

    Mirrors the while-loop in check_v2_signature (up to 10 iterations).
    Returns the raw value bytes of the v2 entry (everything after the 4-byte
    ID field).  Raises ValueError if the entry is not present.
    """
    pos = 8  # skip the leading uint64 size_of_block field

    for _ in range(10):
        if pos + 8 > len(block):
            break

        pair_length = struct.unpack_from("<Q", block, pos)[0]
        pos += 8

        # The trailing size_of_block marker signals end of pairs
        if pair_length == size_of_block:
            break

        if pos + 4 > len(block):
            break

        pair_id = struct.unpack_from("<I", block, pos)[0]
        pos += 4  # consume the 4-byte ID

        value_length = pair_length - 4  # pair_length includes the ID

        if pair_id == _APK_SIG_SCHEME_V2_ID:
            return block[pos : pos + value_length]

        pos += value_length  # skip this pair's value

    raise ValueError("APK Signature Scheme v2 block (ID 0x7109871a) not found")


def _extract_cert_from_v2_value(v2_value: bytes):
    """Extract the first signer's first DER certificate from the v2 value.

    This mirrors check_block() in kernel/apk_sign.c step-for-step:
      1. signer-sequence length  (4 bytes, skipped)
      2. signer length           (4 bytes, skipped)
      3. signed-data length      (4 bytes, skipped)
      4. digests-sequence length (4 bytes, value used to skip digests)
      5. skip <digests-sequence length> bytes
      6. certificates length     (4 bytes, read but immediately overwritten)
      7. certificate length      (4 bytes, used to read the cert)
      8. certificate DER bytes   (<certificate length> bytes, hashed)

    Returns (cert_bytes, cert_length_int).
    Raises ValueError on parse errors or if the cert is oversized.
    """
    pos = 0

    def _u32() -> int:
        nonlocal pos
        if pos + 4 > len(v2_value):
            raise ValueError("Unexpected end of v2 block data")
        val = struct.unpack_from("<I", v2_value, pos)[0]
        pos += 4
        return val

    _u32()  # signer-sequence length  (unused)
    _u32()  # signer length            (unused)
    _u32()  # signed-data length       (unused)

    digests_len = _u32()  # digests-sequence length
    pos += digests_len    # skip the digests sequence

    _u32()              # certificates-sequence length (read but not used)
    cert_len = _u32()   # first certificate length

    if cert_len > _CERT_MAX_LENGTH:
        raise ValueError(
            f"Certificate length {cert_len} exceeds maximum {_CERT_MAX_LENGTH}"
        )
    if pos + cert_len > len(v2_value):
        raise ValueError("Unexpected end of data while reading certificate")

    cert = v2_value[pos : pos + cert_len]
    return cert, cert_len


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def extract_apk_v2_cert_hash(apk_path: str):
    """Return (cert_size: int, cert_sha256_hex: str) for *apk_path*.

    Raises ValueError / OSError on any parse or I/O error.
    """
    with open(apk_path, "rb") as fh:
        data = fh.read()

    cd_offset = _find_cd_offset(data)
    signing_block, size_of_block = _find_signing_block(data, cd_offset)
    v2_value = _find_v2_block_value(signing_block, size_of_block)
    cert, cert_len = _extract_cert_from_v2_value(v2_value)

    cert_hash = hashlib.sha256(cert).hexdigest()
    return cert_len, cert_hash


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------

def main() -> int:
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <apk_file>", file=sys.stderr)
        print("Output: CERT_SIZE=<decimal>  CERT_HASH=<sha256hex>", file=sys.stderr)
        return 1

    apk_path = sys.argv[1]
    if not os.path.isfile(apk_path):
        print(f"Error: file not found: {apk_path}", file=sys.stderr)
        return 1

    try:
        size, sha256 = extract_apk_v2_cert_hash(apk_path)
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1

    # Machine-readable output (parsed by the workflow)
    print(f"CERT_SIZE={size}")
    print(f"CERT_HASH={sha256}")

    # Human-readable header format (informational, goes to stderr)
    print(f"\nFor kernel/kamisu_sign.h:", file=sys.stderr)
    print(f"#define EXPECTED_SIZE_KAMISU 0x{size:03x}", file=sys.stderr)
    print(f'#define EXPECTED_HASH_KAMISU \\', file=sys.stderr)
    print(f'    "{sha256}"', file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
