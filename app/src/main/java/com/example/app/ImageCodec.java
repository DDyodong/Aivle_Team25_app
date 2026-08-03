package com.example.app;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

final class ImageCodec {
    private static final int MAX_IMAGE_SIZE = 1600;
    private static final int JPEG_QUALITY = 88;

    private ImageCodec() {
    }

    static byte[] toUploadJpeg(Bitmap source) {
        Bitmap scaled = downscale(source, MAX_IMAGE_SIZE);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("이미지를 변환하지 못했습니다.", exception);
        } finally {
            if (scaled != source) scaled.recycle();
        }
    }

    private static Bitmap downscale(Bitmap source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (Math.max(width, height) <= maxSize) return source;
        float ratio = maxSize / (float) Math.max(width, height);
        return Bitmap.createScaledBitmap(
                source,
                Math.round(width * ratio),
                Math.round(height * ratio),
                true
        );
    }
}
