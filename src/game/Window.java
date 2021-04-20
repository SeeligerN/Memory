package game;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

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

	private static int turnedPairs = 0;
	private static int cardsTurnedUnnecessarily = 0;
	private static long timeStarted = 0;

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
		timeStarted = System.currentTimeMillis();
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
					textures[card] = ImageIO
							.read(Window.class.getResourceAsStream("/images/" + generateFilename(card) + ".png"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void resetSize() {
		int preferredWidth = width * 150;
		int preferredHeight = height * 225;
		preferredWidth = preferredWidth > 1500 ? 1500 : preferredWidth;
		preferredHeight = preferredHeight > 1000 ? 1000 : preferredHeight;
		label.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public static void generateField() {
		cards = new int[width][height];

		Random r = new Random();
		for (int x = 0; x < cards.length; x++) {
			for (int y = 0; y < cards[x].length; y++) {
				if (cards[x][y] != 0)
					continue;

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

						if (y2 == height)
							y2 = 0;
					}
				}
				cards[x2][y2] = card;
			}
		}
	}

	private static boolean cardsContain(int card) {
		for (int x = 0; x < cards.length; x++) {
			for (int y = 0; y < cards[x].length; y++) {
				if ((cards[x][y] & 0b111111) == (card & 0b111111))
					return true;
			}
		}
		return false;
	}

	public static boolean cardsCleared() {
		for (int x = 0; x < cards.length; x++) {
			for (int y = 0; y < cards[x].length; y++) {
				if ((cards[x][y] & 0b11111) != 0)
					return false;
			}
		}
		return true;
	}

	public static String generateFilename(int card) {
		String filename = "";

		int type = card & 0b001111;
		if (type < 1 | type > 13)
			return filename;
		if (type == 1)
			filename += "A";
		if (type == 11)
			filename += "J";
		if (type == 12)
			filename += "Q";
		if (type == 13)
			filename += "K";
		if (type > 1 && type < 11)
			filename += type;

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

	public static String formatTime(long time) {
		int millis = (int) (time % 1000);
		time /= 1000;
		int seconds = (int) (time % 60);
		time /= 60;
		int minutes = (int) (time % 60);
		time /= 60;
		int hours = (int) (time % 60);

		String timeString = String.format("%02d:%02d.%03ds", minutes, seconds, millis);
		if (hours != 0)
			timeString = String.format("%02d:%s", hours, timeString);

		return timeString;
	}

	private static class DrawLabel extends JLabel {

		private static final long serialVersionUID = 1L;

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			
			int spaceWidth = this.getWidth() / cards.length;
			int spaceHeight = this.getHeight() / cards[0].length;

			for (int x = 0; x < cards.length; x++) {
				for (int y = 0; y < cards[x].length; y++) {
					if (textures[cards[x][y] & 0b11111] != null)
						if ((cards[x][y] & 0b111111) != 0) {
							paintCard(x * spaceWidth, y * spaceHeight, spaceWidth, spaceHeight, g, backTexture);
						}
				}
			}

			if (selected1[0] >= 0 && selected1[1] >= 0)
				if (textures[cards[selected1[0]][selected1[1]] & 0b111111] != null)
					paintCard(selected1[0] * spaceWidth, selected1[1] * spaceHeight, spaceWidth, spaceHeight, g,
							textures[cards[selected1[0]][selected1[1]] & 0b111111]);
			if (selected2[0] >= 0 && selected2[1] >= 0)
				if (textures[cards[selected2[0]][selected2[1]] & 0b111111] != null)
					paintCard(selected2[0] * spaceWidth, selected2[1] * spaceHeight, spaceWidth, spaceHeight, g,
							textures[cards[selected2[0]][selected2[1]] & 0b111111]);
		}

		private void paintCard(int x, int y, int spaceWidth, int spaceHeight, Graphics g, BufferedImage texture) {
			int margin = 5;
			
			x += margin;
			y += margin;
			spaceWidth -= 2 * margin;
			spaceHeight -= 2 * margin;
			
			int cardWidth = spaceWidth;
			int cardHeight = spaceHeight;

			if ((float) cardWidth / cardHeight > (float) texture.getWidth() / texture.getHeight())
				cardWidth = (int) (cardHeight * ((float) texture.getWidth() / texture.getHeight()));
			else
				cardHeight = (int) (cardWidth * ((float) texture.getHeight() / texture.getWidth()));

			x = (x + spaceWidth / 2 - cardWidth / 2);
			y = (y + spaceHeight / 2 - cardHeight / 2);

			g.drawImage(texture, x, y, cardWidth, cardHeight, null);
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

					cards[selected1[0]][selected1[1]] += 1 << 6;
					cards[selected2[0]][selected2[1]] += 1 << 6;
					turnedPairs++;

					if ((cards[selected1[0]][selected1[1]] & 0b111111) == (cards[selected2[0]][selected2[1]]
							& 0b111111)) {
						int tally1 = cards[selected1[0]][selected1[1]] >> 6;
						int tally2 = cards[selected1[0]][selected1[1]] >> 6;

						tally1 = tally1 > 0 ? tally1 - 1 : tally1;
						tally1 = tally1 > 0 ? tally1 - 1 : tally1;
						cardsTurnedUnnecessarily += tally1;
						tally2 = tally2 > 0 ? tally2 - 1 : tally2;
						tally2 = tally2 > 0 ? tally2 - 1 : tally2;
						cardsTurnedUnnecessarily += tally2;

						cards[selected1[0]][selected1[1]] = cards[selected2[0]][selected2[1]] = 0;

						if (cardsCleared()) {
							do {
								if ((float) (width * backTexture.getWidth())
										/ (height * backTexture.getHeight()) < preferedAspectRatio) {
									width++;
								} else {
									height++;
								}
							} while ((width * height) % 2 != 0);

							if (width * height > 52 * 2) {
								// win condition
								long time = System.currentTimeMillis() - timeStarted;
								String message = "Herzlichen Glückwunsch! Du hast so viel Zeit verschwendet, dass es nicht mehr genug \n"
										+ "Karten im Deck gibt um weiter zu spielen. Das ist ein wahres Testament deiner Abneigung gegen produktives Arbeiten. \n"
										+ "Hier ein paar interessante Statistiken zum Angeben: \n";
								message += "Kartenpaare gedreht: " + turnedPairs + "\n";
								message += "Unnötige Drehungen: " + cardsTurnedUnnecessarily + "\n";
								message += "Zeit verschwendet: " + formatTime(time);

								JOptionPane.showMessageDialog(frame, message, "Du hast gewonnen?",
										JOptionPane.INFORMATION_MESSAGE);

								frame.dispose();
							} else {
								generateField();
								if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH)
									resetSize();
							}

						}
					}

					selected1 = new int[] { -1, -1 };
					selected2 = new int[] { -1, -1 };
				} else {
					if (x == selected1[0] && y == selected1[1])
						return;

					if ((cards[x][y] & 0b111111) != 0)
						selected2 = new int[] { x, y };
				}

			} else if ((cards[x][y] & 0b111111) != 0)
				selected1 = new int[] { x, y };

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
