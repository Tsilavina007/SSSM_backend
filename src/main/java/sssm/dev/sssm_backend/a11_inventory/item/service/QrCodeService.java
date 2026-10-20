package sssm.dev.sssm_backend.a11_inventory.item.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

  public byte[] generatePng(String content, int width, int height)
      throws WriterException, IOException {
    QRCodeWriter writer = new QRCodeWriter();
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    hints.put(EncodeHintType.MARGIN, 1);
    BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(matrix, "PNG", out);
    return out.toByteArray();
  }

  public byte[] generatePng(String content) throws WriterException, IOException {
    return generatePng(content, 300, 300);
  }
}
