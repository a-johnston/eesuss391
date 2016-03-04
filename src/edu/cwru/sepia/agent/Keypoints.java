package edu.cwru.sepia.agent;

import java.util.ArrayList;
import java.util.List;

// TODO: We need to integrate this into GameState.java at some point since we can only turn in GameState and MinimaxAlphaBeta 
public class Keypoints {
	static int[] trees = new int[]{4, 3, 4, 4, 4, 5, 4, 6, 4, 7, 4, 8, 4, 9, 4, 10, 5, 10, 6, 10, 7, 10, 8, 10, 9, 10, 10, 10, 10, 6, 10, 7, 10, 8, 10, 9, 12, 6, 12, 7, 12, 8, 12, 9, 12, 10, 13, 10, 14, 10, 15, 10, 16, 10, 17, 10, 18, 10, 19, 10, 19, 9, 19, 8, 19, 7, 19, 6, 19, 5, 19, 4, 19, 3, 4, 12, 4, 13, 4, 14, 5, 14, 6, 14, 7, 15, 8, 16, 9, 17, 10, 18, 13, 18, 14, 17, 15, 16, 16, 15, 17, 14, 19, 13, 19, 14, 18, 14, 19, 12, 10, 12, 10, 13, 10, 14, 10, 15, 12, 12, 12, 13, 12, 14, 12, 15, 12, 16};

	private static class XY {
		final int x;
		final int y;
		
		public XY(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public Keypoints(int[] trees, int xExtent, int yExtent) {
		char[][] map = new char[yExtent][xExtent]; //y, x

		List<XY> keyPoints = new ArrayList<>();

		for (int y = 0; y < yExtent; y++) {
			for (int x = 0; x < xExtent; x++) {
				map[y][x] = ' ';
			}
		}

		for (int i = 0; i < trees.length; i += 2) {
			map[trees[i+1]][trees[i]] = 'T';
		}

		characterize(map);

		for (int y = 0; y < 19; y++) {
			for (int x = 0; x < 25; x++) {
				if (map[y][x] == 'T') {
					map[y][x] = ' ';
				}
			}
		}
//
//		for (int y = 1; y < 18; y++) {
//			for (int x = 1; x < 24; x++) {
//				if (isFree(map, x, y)) {
//					int closestId = closest(map, x, y, keyPoints);
//					if (closestId != -1) {
//						if (closestId > 9) {
//							map[y][x] = (char) ('a' + (closestId - 10));
//						} else {
//							map[y][x] = (char) ('0' + closestId);
//						}
//					} else {
//						map[y][x] = '?';
//					}
//				}
//			}
//		}

		for (int y = 0; y < 19; y++) {
			for (int x = 0; x < 25; x++) {
				System.out.print(map[y][x]);
			}
			System.out.println("");
		}
	}

	public static void main(String[] args) {
		char[][] map = new char[19][25]; //y, x
		
		List<XY> keyPoints = new ArrayList<>();
		
		for (int y = 0; y < 19; y++) {
			for (int x = 0; x < 25; x++) {
				map[y][x] = ' ';
			}
		}
		
		for (int i = 0; i < trees.length; i += 2) {
			map[trees[i+1]][trees[i]] = 'T';
		}
		
		characterize(map);
		
		for (int y = 0; y < 19; y++) {
			for (int x = 0; x < 25; x++) {
				if (map[y][x] == 'T') {
					map[y][x] = ' ';
				}
			}
		}
//		
//		for (int y = 1; y < 18; y++) {
//			for (int x = 1; x < 24; x++) {
//				if (isFree(map, x, y)) {
//					int closestId = closest(map, x, y, keyPoints);
//					if (closestId != -1) {
//						if (closestId > 9) {
//							map[y][x] = (char) ('a' + (closestId - 10));
//						} else {
//							map[y][x] = (char) ('0' + closestId);
//						}
//					} else {
//						map[y][x] = '?';
//					}
//				}
//			}
//		}
		
		for (int y = 0; y < 19; y++) {
			for (int x = 0; x < 25; x++) {
				System.out.print(map[y][x]);
			}
			System.out.println("");
		}
	}
	
	public static List<List<XY>> characterize(char[][] map) {
		int zoneId = 0;
		List<List<XY>> zones = new ArrayList<>();
		
		boolean added;
		int i;
		for (int x = 0; x < map[0].length; x++) {
			for (int y = 0; y < map.length; y++) {
				
				if (map[y][x] == 'T') {
					continue;
				}
				
				added = false;
				i = 0;
				for (List<XY> zone : zones) {
					if (inZone(map, x, y, zone)) {
						zone.add(new XY(x, y));
						map[y][x] = (char)('a' + i);
						added = true;
						break;
					}
					i++;
				}
				if (!added) {
					List<XY> newZone = new ArrayList<>();
					newZone.add(new XY(x, y));
					map[y][x] = (char)('a' + zoneId++);
					zones.add(newZone);
				}
			}
		}
		
		return zones;
	}
	
	public static List<XY>[][] getAdjacent(char[][] map, List<XY> zones) {
		@SuppressWarnings("unchecked")
		List<XY>[][] adjacent = new List[zones.size()][zones.size()];
		
		for (int y = 0; y < map.length; y++) {
			for (int x = 0; x < map[y].length; x++) {
				;
			}
		}
		
		return adjacent;
	}
	
	public static void markAdjacentTo(List<XY>[][] adj) {
		
	}
	
	public static boolean inZone(char[][] map, int x, int y, List<XY> zone) {
		for (XY xy : zone) {
			if (!trivialPathExists(map, x, y, xy.x, xy.y)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean isKey(char[][] map, int x, int y) {
		if (map[y][x] == 'T') {
			return false;
		}
		
		if (map[y-1][x] != map[y][x-1] && map[y+1][x] != map[y][x+1] && map[y-1][x] == map[y+1][x]) {
			return true;
		}
		
		int cardinalSum = 0;
		cardinalSum += map[y-1][x] == 'T' ? 1 : 0;
		cardinalSum += map[y+1][x] == 'T' ? 1 : 0;
		cardinalSum += map[y][x-1] == 'T' ? 1 : 0;
		cardinalSum += map[y][x+1] == 'T' ? 1 : 0;
		
		int sum = 0;
		for (int i = -1; i < 2; i += 1) {
			for (int j = -1; j < 2; j += 1) {
				if (map[y + i][x + j] == 'T') {
					sum += 1;
				}
			}
		}
		
		return sum == 1 && cardinalSum == 1;
	}
	
	public static boolean isFree(char[][] map, int x, int y) {
		for (int i = -1; i < 2; i += 1) {
			for (int j = -1; j < 2; j += 1) {
				if (map[y + i][x + j] == 'T') {
					return false;
				}
			}
		}
		return true;
	}
	
	public static int closest(char[][] map, int x, int y, List<XY> keyPoints) {
		int index = -1;
		int distance = Integer.MAX_VALUE;
		for (int i = 0; i < keyPoints.size(); i++) {
			XY xy = keyPoints.get(i);
			int temp = getDistance(x, y, xy.x, xy.y);
			if (temp < distance && altTrivialPathExists(map, x, y, xy.x, xy.y)) {
				distance = temp;
				index = i;
			}
		}
		return index;
	}
	
	public static int getDistance(int x, int y, int x2, int y2) {
		return Math.abs(x - x2) + Math.abs(y - y2);
	}
	
	public static boolean trivialPathExists(char[][] map, int x1, int y1, int x2, int y2) {
		double t = 0.0;
		
		double tx, ty;
		double dx = x2 - x1;
		double dy = y2 - y1;
		
		double step = 0.5 / Math.sqrt(dx*dx + dy*dy);
		
		while (t <= 1.0) {
			tx = Math.round(x1 + dx * t);
			ty = Math.round(y1 + dy * t);
			
			if (map[(int) ty][(int) tx] == 'T') {
				return false;
			}
			
			t += step;
		}
		
		return true;
	}
	
	public static boolean altTrivialPathExists(char[][] map, int x1, int y1, int x2, int y2) {
		if (map[y1][x1] == 'T') {
			return false;
		}
		
		if (x1 == x2 && y1 == y2) {
			return true;
		}
		
		int dx = Integer.compare(x2, x1);
		int dy = Integer.compare(y2, y1);
		
		return (dx == 0 ? false : altTrivialPathExists(map, x1 + dx, y1, x2, y2))
			|| (dy == 0 ? false : altTrivialPathExists(map, x1, y1 + dy, x2, y2));
	}
}
