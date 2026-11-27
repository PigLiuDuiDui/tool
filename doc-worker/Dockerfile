FROM python:3.11-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libreoffice-writer-nogui libreoffice-impress-nogui libreoffice-calc-nogui \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
ENV UPLOAD_DIR=/data/uploads
EXPOSE 8092
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8092"]
