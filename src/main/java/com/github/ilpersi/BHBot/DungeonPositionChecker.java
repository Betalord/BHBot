package com.github.ilpersi.BHBot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Use this class inside dungeons to understand if the player position has changed. This is useful together with autoShrines
 * to enable shrines as soon as possible
 */
public class DungeonPositionChecker {

    // Width and Height of the sample Image
    final int imgSize = 40;

    // Bounds of the sample images
    final private Bounds topLeftImg = Bounds.fromWidthHeight(95, 15, imgSize, imgSize);
    final private Bounds bottomLeftImg = Bounds.fromWidthHeight(95, 310, imgSize, imgSize);
    final private Bounds topRightImg = Bounds.fromWidthHeight( 650, 15,imgSize, imgSize);
    final private Bounds bottomRightImg = Bounds.fromWidthHeight(650, 310, imgSize, imgSize);

    // Internal image fields to store previous position samples
    private BufferedImage subImgTopLef;
    private BufferedImage subImgBottomLeft;
    private BufferedImage subImgTopRight;
    private BufferedImage subImgBottomRight;

    // Should we reset the previous position samples?
    private boolean resetStartPos;

    // How many checks before we confirm that the position has not changed^
    @SuppressWarnings("FieldCanBeLocal")
    private final int REQUIRED_CNT = 5;

    // Temporary int to save the number of successful position checks
    private int currentCnt = 0;

    DungeonPositionChecker() {
        subImgTopLef = null;
        subImgBottomLeft = null;
        subImgTopRight = null;
        subImgBottomRight = null;

        resetStartPos = true;
    }

    /**
     * @param img a Buffered img to check if the position has changed
     * @return true if the position has not changes for a number of times specified in REQUIRED_CNT
     */
    boolean isSamePosition(BufferedImage img) {
        // When we have to reset the original position, we updated the internal sub images containing the samples
        if (resetStartPos) {
            subImgTopLef = img.getSubimage(topLeftImg.x1, topLeftImg.y1, topLeftImg.width, topLeftImg.height);
            subImgBottomLeft = img.getSubimage(bottomLeftImg.x1, bottomLeftImg.y1, bottomLeftImg.width, bottomLeftImg.height);
            subImgTopRight = img.getSubimage(topRightImg.x1, topRightImg.y1, topRightImg.width, topRightImg.height);
            subImgBottomRight = img.getSubimage(bottomRightImg.x1, bottomRightImg.y1, bottomRightImg.width, bottomRightImg.height);

            resetStartPos = false;
            return false;
        }

        // Temporary samples to do the comparison
        BufferedImage tmpImgTopLef = img.getSubimage(topLeftImg.x1, topLeftImg.y1, topLeftImg.width, topLeftImg.height);
        BufferedImage tmpImgBottomLeft = img.getSubimage(bottomLeftImg.x1, bottomLeftImg.y1, bottomLeftImg.width, bottomLeftImg.height);
        BufferedImage tmpImgTopRight = img.getSubimage(topRightImg.x1, topRightImg.y1, topRightImg.width, topRightImg.height);
        BufferedImage tmpImgBottomRight = img.getSubimage(bottomRightImg.x1, bottomRightImg.y1, bottomRightImg.width, bottomRightImg.height);

        if (MD5Compare(tmpImgTopLef, subImgTopLef) && MD5Compare(tmpImgBottomLeft, subImgBottomLeft) &&
                MD5Compare(tmpImgTopRight, subImgTopRight) && MD5Compare(tmpImgBottomRight, subImgBottomRight)) {
            currentCnt++;
            BHBot.logger.debug("PositionChecker: position has not changed for "  + currentCnt + " time(s).");
        } else {
            currentCnt = 0;
            resetStartPos = true;
            BHBot.logger.debug("PositionChecker: new position detected.");
        }

        return currentCnt >= REQUIRED_CNT;
    }

    /**
     * @param img The BufferedImage that you want to get the hash for
     * @return an array of bytes that contains the MD5 hash
     */
    private byte[] imgToMd5(BufferedImage img) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", outputStream);
        } catch (IOException e) {
            BHBot.logger.error("imgToMd5: impossible to write image to outputStream", e);
            return new byte[] {};
        }
        byte[] data = outputStream.toByteArray();

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            BHBot.logger.error("imgToMd5: error while digesting MD5 hash", e);
            return new byte[] {};
        }
        md.update(data);
        return md.digest();
    }

    /**
     *
     * To compare images we use the MD5: from tests it is approximately 5X faster than pixel comparison and,
     * as images to compare are the same, it is very reliable
     *
     * @param imgActual The image we want to compare with the expected one
     * @param imgExpected The expected image
     * @return true if images are the same
     */
    private boolean MD5Compare(BufferedImage imgActual, BufferedImage imgExpected) {
        return Arrays.equals(imgToMd5(imgActual), imgToMd5(imgExpected));
    }
}