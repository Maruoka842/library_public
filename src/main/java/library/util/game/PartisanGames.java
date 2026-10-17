package library.util.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import library.util.DyadicRational;

/**
 * 代表的な非不偏ゲームの実装集。
 *
 * <p>非不偏ゲーム (Partisan Games) は、プレイヤーによって可能な遷移が異なるゲームである。
 * コンビナトリアル・ゲーム理論の枠組みで、数（Numbers）やゲーム値として解析される。</p>
 */
public class PartisanGames {

	/**
	 * 複数の非不偏ゲームの和 (Sum of Partisan Games)。
	 *
	 * <p>各手番において、プレイヤーはいずれか1つのコンポーネント・ゲームを選び、
	 * そのゲームにおいて自分ができる手を選んで進める。
	 * いずれのコンポーネントでも手が打てなくなったプレイヤーが負けとなる。</p>
	 *
	 * @param <S> 状態の型
	 */
	public static class Sum<S> extends PartisanGame<List<S>> {
		private final List<? extends PartisanGame<? extends S>> games;

		public Sum(List<? extends PartisanGame<? extends S>> games) {
			this.games = games;
		}

		/**
		 * 異なる状態の型（例：LongとIntegerなど）を持つ複数のゲームの和（Sum<Object>）を生成する。
		 *
		 * @param games 各ゲームのインスタンス
		 * @return Objectを状態の型とする和ゲームのインスタンス
		 *
		 * <p>【契約】
		 * <ul>
		 *   <li>事前条件: games が null でないこと。</li>
		 *   <li>事後条件: Sum<Object> インスタンスを生成して返す。</li>
		 *   <li>計算量: O(N) （Nはゲーム数）。</li>
		 * </ul>
		 * </p>
		 */
		// 未テスト
		public static Sum<Object> of(PartisanGame<?>... games) {
			List<PartisanGame<?>> list = new ArrayList<>();
			for (PartisanGame<?> game : games) {
				list.add(game);
			}
			return new Sum<>(list);
		}

		@SuppressWarnings("unchecked")
		private PartisanGame<S> getGame(int i) {
			return (PartisanGame<S>) games.get(i);
		}

		@Override
		public Iterable<List<S>> nextStatesLeft(List<S> states) {
			List<List<S>> nexts = new ArrayList<>();
			for (int i = 0; i < games.size(); i++) {
				PartisanGame<S> game = getGame(i);
				for (S nextState : game.nextStatesLeft(states.get(i))) {
					List<S> nextList = new ArrayList<>(states);
					nextList.set(i, nextState);
					nexts.add(nextList);
				}
			}
			return nexts;
		}

		@Override
		public Iterable<List<S>> nextStatesRight(List<S> states) {
			List<List<S>> nexts = new ArrayList<>();
			for (int i = 0; i < games.size(); i++) {
				PartisanGame<S> game = getGame(i);
				for (S nextState : game.nextStatesRight(states.get(i))) {
					List<S> nextList = new ArrayList<>(states);
					nextList.set(i, nextState);
					nexts.add(nextList);
				}
			}
			return nexts;
		}

		/**
		 * 和ゲームのゲーム値を計算する。
		 * ゲーム値vについて
		 * v > 0 ならば L 勝ち
		 * v < 0 ならば R 勝ち
		 * v == 0 ならば　後手勝ち
		 * @param states 各ゲームの状態のリスト
		 * @return 各ゲームのゲーム値の和。いずれかのゲーム値が定義されない場合は null。
		 *
		 * <p>【契約】
		 * <ul>
		 *   <li>事前条件: states が null でないこと。</li>
		 *   <li>事後条件: dyadic rational 値または null を返す。</li>
		 *   <li>計算量: O(N * C)（N: ゲームの数, C: 各ゲーム of 状態 の計算量）。</li>
		 * </ul>
		 * </p>
		 */
		// 未テスト
		@Override
		public DyadicRational gameValue(List<S> states) {
			DyadicRational sum = new DyadicRational(0);
			for (int i = 0; i < games.size(); i++) {
				PartisanGame<S> game = getGame(i);
				DyadicRational val = game.gameValue(states.get(i));
				if (val == null) {
					return null;
				}
				sum = sum.add(val);
			}
			return sum;
		}

		/**
		 * 和ゲームにおいて先手（手番プレイヤー）が勝ちの場合、1つの最善手（勝利手）を返す。
		 *
		 * @param states 現在の状態のリスト
		 * @param leftTurn Left の手番なら true, Right の手番なら false
		 * @return [gameIndex, nextState] (i番目のゲームの状態を nextState に変更する手)。
		 *         後手勝ち（手番プレイヤーが勝てない場合）は null を返す。
		 * <p>【契約】
		 * <ul>
		 *   <li>事前条件: states が null でないこと。</li>
		 *   <li>事後条件: 最善手（コンポーネントインデックスと遷移先状態）または null を返す。</li>
		 *   <li>計算量: O(Σ M_i * C_i)（ここで M_i は次の状態数、C_i は判定コスト）。</li>
		 * </ul>
		 * </p>
		 */
		// 未テスト
		public Object[] winningMove(List<S> states, boolean leftTurn) {
			if (!canWin(states, leftTurn)) return null;
			for (int i = 0; i < games.size(); i++) {
				PartisanGame<S> game = getGame(i);
				Iterable<S> nexts = leftTurn ? game.nextStatesLeft(states.get(i))
				                             : game.nextStatesRight(states.get(i));
				for (S nextState : nexts) {
					List<S> nextList = new ArrayList<>(states);
					nextList.set(i, nextState);
					if (!canWin(nextList, !leftTurn)) {
						return new Object[]{i, nextState};
					}
				}
			}
			return null;
		}
	}

	/**
	 * 非不偏引き算ゲーム (Partisan Subtraction Game)。
	 *
	 * <p>1つの山から、Left と Right でそれぞれ異なる集合 S_L, S_R に含まれる数だけ石を取り除くことができる。
	 * 石を取ることができなくなったプレイヤーが負けとなる。</p>
	 */
	public static class PartisanSubtractionGame extends PartisanGame<Long> {
		private final List<Long> sL;
		private final List<Long> sR;

		public PartisanSubtractionGame(List<Long> sL, List<Long> sR) {
			this.sL = new ArrayList<>(sL);
			this.sR = new ArrayList<>(sR);
			Collections.sort(this.sL);
			Collections.sort(this.sR);
		}

		@Override
		public Iterable<Long> nextStatesLeft(Long n) {
			List<Long> nexts = new ArrayList<>();
			for (long move : sL) {
				if (n >= move) nexts.add(n - move);
				else break;
			}
			return nexts;
		}

		@Override
		public Iterable<Long> nextStatesRight(Long n) {
			List<Long> nexts = new ArrayList<>();
			for (long move : sR) {
				if (n >= move) nexts.add(n - move);
				else break;
			}
			return nexts;
		}
	}

	/**
	 * ドミニアリング (Domineering)。
	 *
	 * <p>R x C のグリッド上に、Left は 2x1（垂直）、Right は 1x2（水平）のドミノを置いていく。
	 * ドミノを置けなくなったプレイヤーが負けとなる。</p>
	 */
	public static class Domineering extends PartisanGame<Long> {
		private final int rows;
		private final int cols;

		public Domineering(int rows, int cols) {
			this.rows = rows;
			this.cols = cols;
		}

		@Override
		public Iterable<Long> nextStatesLeft(Long board) {
			List<Long> nexts = new ArrayList<>();
			for (int r = 0; r < rows - 1; r++) {
				for (int c = 0; c < cols; c++) {
					long m1 = 1L << (r * cols + c);
					long m2 = 1L << ((r + 1) * cols + c);
					if ((board & m1) == 0 && (board & m2) == 0) {
						nexts.add(board | m1 | m2);
					}
				}
			}
			return nexts;
		}

		@Override
		public Iterable<Long> nextStatesRight(Long board) {
			List<Long> nexts = new ArrayList<>();
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols - 1; c++) {
					long m1 = 1L << (r * cols + c);
					long m2 = 1L << (r * cols + c + 1);
					if ((board & m1) == 0 && (board & m2) == 0) {
						nexts.add(board | m1 | m2);
					}
				}
			}
			return nexts;
		}
	}

	/**
	 * Alice と Bob によるグリッド上の移動ゲーム。
	 *
	 * <p>N x N のグリッド上で駒を動かす。Alice (Left) は(±1, 0)の移動ができる。Bob (Right) は上下左右に動かせる。
	 * 指定された合計手番数 k 回行った後、マスがTrueなら Alice の勝ち、マスがFalseなら Bob の勝ち。</p>
	 *
	 * <p>未テスト</p>
	 */
	public static class AliceBobGridGame extends PartisanGame<AliceBobGridGame.State> {
		/**
		 * ゲームの状態を表すレコード。
		 * (r, c)が現在の位置。
		 * @param r 行インデックス
		 * @param c 列インデックス
		 * @param k 残りの合計手番数
		 */
		public record State(int r, int c, int k) {
		}

		private final int n;
		private final boolean[][] isWhite;

		/**
		 * AliceBobGridGame を構築する。
		 *
		 * @param n       グリッドのサイズ
		 * @param isWhite 各マスが白かどうか (true: 白, false: 黒)
		 *
		 *                <p>【契約】
		 *                <ul>
		 *                  <li>事前条件: n >= 2, isWhite は n x n の配列であること。</li>
		 *                  <li>事後条件: インスタンスを生成する。</li>
		 *                  <li>計算量: O(1)</li>
		 *                  <li>参照共有・所有権: isWhite は参照として保持される。</li>
		 *                </ul>
		 *                </p>
		 */
		public AliceBobGridGame(boolean[][] isWhite) {
			n=isWhite.length;
			this.isWhite = isWhite;
		}

		@Override
		public Iterable<State> nextStatesLeft(State state) {
			if (state.k <= 0) return List.of();
			List<State> nexts = new ArrayList<>();
			if (state.r > 0) nexts.add(new State(state.r-1, state.c, state.k - 1));
			if (state.r < n - 1) nexts.add(new State(state.r+1, state.c, state.k - 1));
			return nexts;
		}

		@Override
		public Iterable<State> nextStatesRight(State state) {
			if (state.k <= 0) return List.of();
			List<State> nexts = new ArrayList<>();
			if (state.r > 0) nexts.add(new State(state.r - 1, state.c, state.k - 1));
			if (state.r < n - 1) nexts.add(new State(state.r + 1, state.c, state.k - 1));
			if (state.c > 0) nexts.add(new State(state.r, state.c - 1, state.k - 1));
			if (state.c < n - 1) nexts.add(new State(state.r, state.c + 1, state.k - 1));
			return nexts;
		}
		
		@Override
		protected Winner terminalWinner(State state) {
			return isWhite[state.r][state.c] ? Winner.LEFT : Winner.RIGHT;
		}

	}

	/**
	 * Left と Right がそれぞれ最大1回だけ「パス」ができる非不偏ゲーム。
	 *
	 * <p>パスを行うと、状態は変更されず、パス権が消費される。
	 * 手番をパスすることしかできず、パス権がない場合は移動できない。</p>
	 */
	public static class PassOnlyGame extends PartisanGame<PassOnlyGame.State> {
		/**
		 * PassOnlyGame の状態を表すレコード。
		 * @param leftPassAvailable Left がパス可能か
		 * @param rightPassAvailable Right がパス可能か
		 */
		public record State(boolean leftPassAvailable, boolean rightPassAvailable) {}

		/**
		 * PassOnlyGame を構築する。
		 *
		 * <p>【契約】
		 * <ul>
		 *   <li>事前条件: なし。</li>
		 *   <li>事後条件: インスタンスを生成する。</li>
		 *   <li>計算量: O(1)</li>
		 * </ul>
		 * </p>
		 */
		public PassOnlyGame() {}

		@Override
		public Iterable<State> nextStatesLeft(State state) {
			if (state.leftPassAvailable) {
				return List.of(new State(false, state.rightPassAvailable));
			}
			return List.of();
		}

		@Override
		public Iterable<State> nextStatesRight(State state) {
			if (state.rightPassAvailable) {
				return List.of(new State(state.leftPassAvailable, false));
			}
			return List.of();
		}
	}
}
