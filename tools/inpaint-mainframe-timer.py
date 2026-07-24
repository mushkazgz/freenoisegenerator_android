"""Remove the obsolete timer numbers from the flattened mainframe artwork."""

from pathlib import Path
import sys

import cv2
import numpy as np


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: inpaint-mainframe-timer.py <image.png>")

    image_path = Path(sys.argv[1])
    image = cv2.imread(str(image_path), cv2.IMREAD_COLOR)
    if image is None:
        raise SystemExit(f"Could not read {image_path}")

    mask = np.zeros(image.shape[:2], dtype=np.uint8)
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    number_regions = (
        (587, 957, 610, 978),
        (751, 957, 778, 978),
        (606, 1035, 624, 1057),
        (732, 1035, 760, 1057),
    )

    for left, top, right, bottom in number_regions:
        bright_text = gray[top:bottom, left:right] > 58
        mask[top:bottom, left:right][bright_text] = 255

    mask = cv2.dilate(mask, np.ones((3, 3), np.uint8), iterations=1)
    cleaned = cv2.inpaint(image, mask, 4, cv2.INPAINT_TELEA)
    if not cv2.imwrite(str(image_path), cleaned):
        raise SystemExit(f"Could not write {image_path}")


if __name__ == "__main__":
    main()
