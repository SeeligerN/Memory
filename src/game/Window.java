package game;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Window {

	private static float preferedAspectRatio = 16f / 9f;
	private static int width = 3;
	private static int height = 2;

	private static BufferedImage[] textures;
	private static BufferedImage backTexture;
	private static int[][] cards;

	private static int[] selected1;
	private static int[] selected2;

	private static JFrame frame;
	private static DrawLabel label;

	public static void main(String[] args) {
		loadTextures();
		generateField();

		selected1 = new int[] { -1, -1 };
		selected2 = new int[] { -1, -1 };

		frame = new JFrame("Memory");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		label = new DrawLabel();
		label.addMouseListener(new MouseListener());
		frame.add(label);

		resetSize();
		frame.setVisible(true);
	}

	public static void loadTextures() {
		try {
			backTexture = ImageIO.read(Window.class.getResourceAsStream("/images/red_back.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		textures = new BufferedImage[64];
		for (int color = 0; color < 4; color++) {
			for (int type = 1; type < 14; type++) {
				int card = (color << 4) + type;

				try {
					textures[card] = ImageIO.read(Window.class.getResourceAsStream("/images/" + generateFilename(card) + ".png"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void resetSize() {
		label.setPreferredSize(new Dimension(width * 150, height * 225));
		frame.pack();
		frame.setLocationRelativeTo(null);
	}
	
	public static void generateField() {
		cards = new int[width][height];

		Random r = new Random();
		for (int x = 0; x < cards.length; x++) {
			for (int y = 0; y < cards[x].length; y++) {
				if (cards[x][y] != 0) continue;

				int card = (r.nextInt(4) << 4) + r.nextInt(13) + 1;
				while (cardsContain(card)) {
					while (generateFilename(++card).equals("")) {
						card++;
						card %= textures.length;
					}
				}
				cards[x][y] = card;

				int x2 = r.nextInt(width);
				int y2 = r.nextInt(height);
				while (cards[x2][y2] != 0) {
					x2++;
					if (x2 == width) {
						x2 = 0;
						y2++;

						if (y2 == height) y2 = 0;
					}
				}
				cards[x2][y2] = card;
			}
		}
	}

	private static boolean cardsContain(int card) {
		for (int x = 0; x < cards.length; x++) {
			for (int y = 0; y < cards[x].length; y++) {
				if (cards[x][y] == card) return true;
			}
		}
		return false;
	}

	public static boolean cardsCleared() {
		for (int x = 0; x < cards.length; x++) {
			for (int y = 0; y < cards[x].length; y++) {
				if (cards[x][y] != 0) return false;
			}
		}
		return true;
	}

	public static String generateFilename(int card) {
		String filename = "";

		int type = card & 0b001111;
		if (type < 1 | type > 13) return filename;
		if (type == 1) filename += "A";
		if (type == 11) filename += "J";
		if (type == 12) filename += "Q";
		if (type == 13) filename += "K";
		if (type > 1 && type < 11) filename += type;

		int color = card & 0b110000;
		color >>= 4;
		switch (color) {
		case 0 -> filename += "C";
		case 1 -> filename += "D";
		case 2 -> filename += "H";
		case 3 -> filename += "S";
		}

		return filename;
	}

	private static class DrawLabel extends JLabel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			int cardWidth = this.getWidth() / cards.length;
			int cardHeight = this.getHeight() / cards[0].length;

			for (int x = 0; x < cards.length; x++) {
				for (int y = 0; y < cards[x].length; y++) {
					if (textures[cards[x][y]] != null) if (cards[x][y] != 0)
						g.drawImage(backTexture, x * cardWidth, y * cardHeight, cardWidth, cardHeight, null);
				}
			}

			if (selected1[0] >= 0 && selected1[1] >= 0) if (textures[cards[selected1[0]][selected1[1]]] != null)
				g.drawImage(textures[cards[selected1[0]][selected1[1]]], selected1[0] * cardWidth,
						selected1[1] * cardHeight, cardWidth, cardHeight, null);
			if (selected2[0] >= 0 && selected2[1] >= 0) if (textures[cards[selected2[0]][selected2[1]]] != null) {
				g.drawImage(textures[cards[selected2[0]][selected2[1]]], selected2[0] * cardWidth,
						selected2[1] * cardHeight, cardWidth, cardHeight, null);
			}
		}
	}

	private static class MouseListener implements java.awt.event.MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {

		}

		@Override
		public void mousePressed(MouseEvent e) {
			int cardWidth = label.getWidth() / cards.length;
			int cardHeight = label.getHeight() / cards[0].length;

			int x = e.getX() / cardWidth;
			int y = e.getY() / cardHeight;

			if (selected1[0] >= 0 && selected1[1] >= 0) {
				if (selected2[0] >= 0 && selected2[1] >= 0) {
					if (cards[selected1[0]][selected1[1]] == cards[selected2[0]][selected2[1]]) {
						cards[selected1[0]][selected1[1]] = cards[selected2[0]][selected2[1]] = 0;

						if (cardsCleared()) {
							do {
								if ((float) width / height < preferedAspectRatio) {
									width++;
								} else {
									height++;
								}
							} while ((width * height) % 2 != 0);
							generateField();
							resetSize();
						}
					}

					selected1 = new int[] { -1, -1 };
					selected2 = new int[] { -1, -1 };
				} else {
					if (x == selected1[0] && y == selected1[1]) return;

					if (cards[x][y] != 0) selected2 = new int[] { x, y };
				}

			} else if (cards[x][y] != 0) selected1 = new int[] { x, y };

			frame.repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

	}
}
