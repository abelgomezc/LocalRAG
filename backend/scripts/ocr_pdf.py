#!/usr/bin/env python3
import sys, os, tempfile, subprocess

pdf_path = sys.argv[1]
tessdata_path = sys.argv[2] if len(sys.argv) > 2 else os.environ.get("TESSDATA_PREFIX", "")

os.environ["TESSDATA_PREFIX"] = tessdata_path

tesseract_exe = os.environ.get("TESSERACT_PATH", "C:/Program Files/Tesseract-OCR/tesseract.exe")

doc = __import__("fitz").open(pdf_path)
output = []

for i, page in enumerate(doc):
    pix = page.get_pixmap(dpi=300)
    img_path = os.path.join(tempfile.gettempdir(), f"_ocr_page_{i}.png")
    pix.save(img_path)
    result = subprocess.run(
        [tesseract_exe, img_path, "stdout", "-l", "spa", "--psm", "6"],
        capture_output=True, text=True, timeout=300
    )
    output.append(result.stdout)
    try:
        os.remove(img_path)
    except OSError:
        pass

print("\n".join(output), end="")
