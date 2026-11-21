package library.util.linalg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import library.util.algebra.instance.impl.ComplexNumber;

/**
 * 1量子ビットの代表的な量子ゲートを表す 2x2 行列。
 */
public final class QuantumGates {
	/** Pauli 作用の単位元。各成分は X,Y,Z の像を表す。 */
	private static final int[] PAULI_IDENTITY = { 1, 2, 3 };
	/** Hadamard ゲートによる X,Y,Z の像。 */
	private static final int[] PAULI_H = { 3, -2, 1 };
	/** S ゲートによる X,Y,Z の像。 */
	private static final int[] PAULI_S = { 2, -1, 3 };
	/** Pauli-X ゲートによる X,Y,Z の像。 */
	private static final int[] PAULI_X = { 1, -2, -3 };
	/** Pauli-Y ゲートによる X,Y,Z の像。 */
	private static final int[] PAULI_Y = { -1, 2, -3 };
	/** Pauli-Z ゲートによる X,Y,Z の像。 */
	private static final int[] PAULI_Z = { -1, -2, 3 };

	private static final ComplexNumber ZERO = ComplexNumber.ZERO;
	private static final ComplexNumber ONE = ComplexNumber.ONE;
	private static final ComplexNumber MINUS_ONE = new ComplexNumber(-1, 0);
	private static final double INV_SQRT_TWO = 1.0 / Math.sqrt(2.0);

	/** 恒等ゲート I。 */
	public static final ComplexNumber[][] I = {
			{ ONE, ZERO },
			{ ZERO, ONE }
	};

	/** Pauli-X ゲート。 */
	public static final ComplexNumber[][] X = {
			{ ZERO, ONE },
			{ ONE, ZERO }
	};

	/** Pauli-Y ゲート。 */
	public static final ComplexNumber[][] Y = {
			{ ZERO, new ComplexNumber(0, -1) },
			{ new ComplexNumber(0, 1), ZERO }
	};

	/** Pauli-Z ゲート。 */
	public static final ComplexNumber[][] Z = {
			{ ONE, ZERO },
			{ ZERO, MINUS_ONE }
	};

	/** Hadamard ゲート H。 */
	public static final ComplexNumber[][] H = {
			{ new ComplexNumber(INV_SQRT_TWO, 0), new ComplexNumber(INV_SQRT_TWO, 0) },
			{ new ComplexNumber(INV_SQRT_TWO, 0), new ComplexNumber(-INV_SQRT_TWO, 0) }
	};

	/** 位相ゲート S = diag(1, i)。 */
	public static final ComplexNumber[][] S = {
			{ ONE, ZERO },
			{ ZERO, ComplexNumber.I }
	};

	/** 位相ゲート S の随伴 S† = diag(1, -i)。 */
	public static final ComplexNumber[][] S_DAGGER = {
			{ ONE, ZERO },
			{ ZERO, new ComplexNumber(0, -1) }
	};

	/** T ゲート = diag(1, exp(i*pi/4))。 */
	public static final ComplexNumber[][] T = {
			{ ONE, ZERO },
			{ ZERO, new ComplexNumber(INV_SQRT_TWO, INV_SQRT_TWO) }
	};

	/** T ゲートの随伴 T† = diag(1, exp(-i*pi/4))。 */
	public static final ComplexNumber[][] T_DAGGER = {
			{ ONE, ZERO },
			{ ZERO, new ComplexNumber(INV_SQRT_TWO, -INV_SQRT_TWO) }
	};

	/** sqrt(X) ゲート。 */
	public static final ComplexNumber[][] SQRT_X = {
			{ new ComplexNumber(0.5, 0.5), new ComplexNumber(0.5, -0.5) },
			{ new ComplexNumber(0.5, -0.5), new ComplexNumber(0.5, 0.5) }
	};

	/**
	 * 全体位相を同一視した Clifford 作用と、それを生成する最短ゲート列。
	 * 像の値は +1=X, -1=-X, +2=Y, -2=-Y, +3=Z, -3=-Z を表す。
	 * @param imageOfX U X U† の像
	 * @param imageOfY U Y U† の像
	 * @param imageOfZ U Z U† の像
	 * @param sequence 入力された生成元を左から適用する最短列。単位元は空文字列
	 */
	public record CliffordElement(int imageOfX, int imageOfY, int imageOfZ, String sequence) {}

	/**
	 * 指定された Clifford 生成元から到達可能な全要素と、各要素への最短列を返す。
	 * 生成元のコストはすべて 1 とし、同じ Pauli 作用を持つ行列は全体位相にかかわらず同一視する。
	 * 生成元として "H", "S", "X", "Y", "Z" を受け付け、重複は一つにまとめる。
	 * @param generators 使用する生成元
	 * @return 探索で到達した各要素と、その要素への最短操作列
	 * @throws IllegalArgumentException 未対応の生成元名が含まれる場合
	 * @complexity O(24 * g * l)。g は異なる生成元数、l は最短列の最大長
	 */
	// 未テスト
	public static List<CliffordElement> shortestSequences(String... generators) {
		LinkedHashSet<String> uniqueGenerators = new LinkedHashSet<>();
		for (String generator : generators) {
			if (!isCliffordGenerator(generator)) {
				throw new IllegalArgumentException("Unsupported Clifford generator: " + generator);
			}
			uniqueGenerators.add(generator);
		}

		List<int[]> states = new ArrayList<>(24);
		List<String> sequences = new ArrayList<>(24);
		Map<String, Integer> stateIds = new HashMap<>();
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		states.add(PAULI_IDENTITY.clone());
		sequences.add("");
		stateIds.put(pauliActionKey(PAULI_IDENTITY), 0);
		queue.add(0);

		while (!queue.isEmpty()) {
			int stateId = queue.removeFirst();
			int[] state = states.get(stateId);
			for (String generator : uniqueGenerators) {
				int[] next = compose(pauliAction(generator), state);
				String key = pauliActionKey(next);
				if (stateIds.containsKey(key)) continue;
				int nextId = states.size();
				stateIds.put(key, nextId);
				states.add(next);
				sequences.add(sequences.get(stateId) + generator);
				queue.addLast(nextId);
			}
		}

		List<CliffordElement> result = new ArrayList<>(states.size());
		for (int i = 0; i < states.size(); i++) {
			int[] state = states.get(i);
			result.add(new CliffordElement(state[0], state[1], state[2], sequences.get(i)));
		}
		return List.copyOf(result);
	}

	/**
	 * 名前が対応する1量子ビット Clifford ゲートかを判定する。
	 * @complexity O(1)
	 */
	// 未テスト
	private static boolean isCliffordGenerator(String generator) {
		return "H".equals(generator) || "S".equals(generator) || "X".equals(generator)
				|| "Y".equals(generator) || "Z".equals(generator);
	}

	/**
	 * ゲート名に対応する Pauli 共役作用を返す。
	 * @complexity O(1)
	 */
	// 未テスト
	private static int[] pauliAction(String generator) {
		return switch (generator) {
		case "H" -> PAULI_H;
		case "S" -> PAULI_S;
		case "X" -> PAULI_X;
		case "Y" -> PAULI_Y;
		case "Z" -> PAULI_Z;
		default -> throw new IllegalArgumentException("Unsupported Clifford generator: " + generator);
		};
	}

	/**
	 * 外側の Pauli 作用を内側の作用の後に合成する。
	 * @complexity O(1)
	 */
	// 未テスト
	private static int[] compose(int[] outer, int[] inner) {
		int[] result = new int[3];
		for (int i = 0; i < 3; i++) {
			int image = inner[i];
			result[i] = Integer.signum(image) * outer[Math.abs(image) - 1];
		}
		return result;
	}

	/**
	 * Pauli 作用の整数キーを作る。
	 * @complexity O(1)
	 */
	// 未テスト
	private static String pauliActionKey(int[] action) {
		return Arrays.toString(action);
	}

	/**
	 * インスタンス化を禁止する。
	 * 計算量: O(1)。
	 */
	// 未テスト
	private QuantumGates() {}
}
