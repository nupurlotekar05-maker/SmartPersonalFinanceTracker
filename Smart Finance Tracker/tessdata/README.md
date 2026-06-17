# Tesseract OCR Data Directory

## Setup Instructions (REQUIRED for OCR to work)

### Step 1: Install Tesseract

**Windows:**
- Download installer: https://github.com/UB-Mannheim/tesseract/wiki
- Install to default location: C:\Program Files\Tesseract-OCR

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-eng
```

**Mac:**
```bash
brew install tesseract
```

### Step 2: Download Language Data

Download `eng.traineddata` from:
https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

Place it in this `tessdata/` folder:
```
tessdata/
  eng.traineddata   ← required
```

### Step 3: Update application.properties

Make sure this is set correctly:
```properties
tesseract.data.path=tessdata
tesseract.language=eng
```

If Tesseract is installed system-wide (Linux/Mac), you can also use:
```properties
tesseract.data.path=/usr/share/tesseract-ocr/5/tessdata
```

### Testing OCR

Upload a clear, well-lit receipt photo via:
```
POST /api/transactions/{id}/receipt
Content-Type: multipart/form-data
file: <your receipt image>
```

The response will include extracted amount, merchant name, and OCR notes.
