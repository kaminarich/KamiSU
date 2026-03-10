#!/usr/bin/env python3
"""Rewrite EXPECTED_SIZE_KAMISU and EXPECTED_HASH_KAMISU in kamisu_sign.h.

Usage:
    python3 update_kamisu_sign.py <kamisu_sign.h> <cert_size> <cert_hash>

Arguments:
    kamisu_sign.h  Path to the header file to update (modified in-place).
    cert_size      Certificate size as a decimal integer.
    cert_hash      SHA-256 hex string (64 lowercase hex characters).

The file is rewritten atomically (write to a temporary file, then replace).
Exit code is non-zero on any error.
"""

import os
import re
import sys


def update_kamisu_sign(header_path: str, cert_size: int, cert_hash: str) -> None:
    """Update EXPECTED_SIZE_KAMISU and EXPECTED_HASH_KAMISU in *header_path*.

    Both values are updated with regex substitution so that the rest of the
    file (comments, include guards, whitespace) is preserved verbatim.
    Raises ValueError if either pattern is not found after the update.
    """
    with open(header_path, "r", encoding="utf-8") as fh:
        content = fh.read()

    size_hex = f"0x{cert_size:03x}"

    # Update: #define EXPECTED_SIZE_KAMISU <old-value>
    new_content = re.sub(
        r"(?m)^(#define EXPECTED_SIZE_KAMISU) \S+$",
        rf"\g<1> {size_hex}",
        content,
    )

    # Update the multi-line hash macro:
    #   #define EXPECTED_HASH_KAMISU \
    #       "<old-hash>"
    # The backslash + newline + optional whitespace + opening quote must all
    # be captured so only the hash hex-string itself is replaced.
    new_content = re.sub(
        r'(#define EXPECTED_HASH_KAMISU\s*\\\n\s*")[^"]+"',
        r"\g<1>" + cert_hash + '"',
        new_content,
    )

    # Sanity-check: both defines must now contain the new values
    if size_hex not in new_content:
        raise ValueError(
            f"Failed to update EXPECTED_SIZE_KAMISU to {size_hex} in {header_path}"
        )
    if cert_hash not in new_content:
        raise ValueError(
            f"Failed to update EXPECTED_HASH_KAMISU to {cert_hash} in {header_path}"
        )

    # Atomic write via a sibling temp file
    tmp_path = header_path + ".tmp"
    try:
        with open(tmp_path, "w", encoding="utf-8") as fh:
            fh.write(new_content)
        os.replace(tmp_path, header_path)
    except Exception:
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)
        raise


def main() -> int:
    if len(sys.argv) != 4:
        print(
            f"Usage: {sys.argv[0]} <kamisu_sign.h> <cert_size> <cert_hash>",
            file=sys.stderr,
        )
        return 1

    header_path = sys.argv[1]
    raw_size = sys.argv[2]
    cert_hash = sys.argv[3]

    if not os.path.isfile(header_path):
        print(f"Error: file not found: {header_path}", file=sys.stderr)
        return 1

    try:
        cert_size = int(raw_size)
    except ValueError:
        print(f"Error: cert_size must be a decimal integer, got: {raw_size!r}", file=sys.stderr)
        return 1

    if len(cert_hash) != 64 or not all(c in "0123456789abcdef" for c in cert_hash):
        print(
            f"Error: cert_hash must be 64 lowercase hex chars, got: {cert_hash!r}",
            file=sys.stderr,
        )
        return 1

    try:
        update_kamisu_sign(header_path, cert_size, cert_hash)
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1

    print(f"Updated {header_path}:")
    print(f"  EXPECTED_SIZE_KAMISU = 0x{cert_size:03x}  ({cert_size})")
    print(f"  EXPECTED_HASH_KAMISU = {cert_hash}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
