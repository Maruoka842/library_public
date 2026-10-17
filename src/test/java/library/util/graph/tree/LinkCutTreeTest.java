package library.util.graph.tree;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.LongBinaryOperator;
import library.util.algebra.strategy.longs.LongMonoidStrategy;
import library.util.algebra.strategy.longs.LongMonoidActionStrategy;

public class LinkCutTreeTest {

    private static final long MOD = 998244353L;

    // Helper functions for Affine Transformation packing/unpacking
    private static long pack(long a, long b) {
        return (a << 32) | (b & 0xFFFFFFFFL);
    }

    private static long getA(long p) {
        return p >>> 32;
    }

    private static long getB(long p) {
        return p & 0xFFFFFFFFL;
    }

    @Test
    public void testBasicForestOperations() {
        int n = 5;
        LinkCutTree lct = new LinkCutTree(n);

        // Initially all vertices are their own roots
        for (int i = 0; i < n; i++) {
            assertEquals(i, lct.root(i));
            assertEquals(0, lct.depth(i));
            assertEquals(-1, lct.parent(i));
        }

        // Link 0 - 1
        assertTrue(lct.link(0, 1));
        assertTrue(lct.same(0, 1));
        assertFalse(lct.same(0, 2));

        // After linking 0 and 1: evert(0) sets 0 as root of its tree, then links 0 to 1.
        // So 1 is the parent of 0.
        assertEquals(1, lct.parent(0));
        assertEquals(1, lct.root(0));
        assertEquals(1, lct.root(1));
        assertEquals(1, lct.depth(0));
        assertEquals(0, lct.depth(1));

        // Link 1 - 2
        assertTrue(lct.link(1, 2));
        assertTrue(lct.same(0, 2));
        assertEquals(2, lct.root(0));

        // Link 3 - 4
        assertTrue(lct.link(3, 4));
        assertFalse(lct.same(0, 3));

        // Cut 1 - 2
        assertTrue(lct.cut(1, 2));
        assertFalse(lct.same(0, 2));
        assertTrue(lct.same(0, 1));

        // Try duplicate cut
        assertFalse(lct.cut(1, 2));
    }

    @Test
    public void testLCAAndAncestors() {
        // Tree:
        //     0
        //    / \
        //   1   2
        //  /
        // 3
        LinkCutTree lct = new LinkCutTree(4);

        assertTrue(lct.link(1, 0));
        assertTrue(lct.link(2, 0));
        assertTrue(lct.link(3, 1));

        // Check LCA
        assertEquals(0, lct.lca(2, 3));
        assertEquals(1, lcaHelp(lct, 1, 3));
        assertEquals(3, lcaHelp(lct, 3, 3));

        // Check Ancestors
        lct.evert(0);
        assertEquals(0, lct.kthAncestor(3, 2));
        assertEquals(1, lct.kthAncestor(3, 1));
        assertEquals(3, lct.kthAncestor(3, 0));
        assertEquals(-1, lct.kthAncestor(3, 3));
    }

    private int lcaHelp(LinkCutTree lct, int u, int v) {
        return lct.lca(u, v);
    }

    @Test
    public void testNonCommutativeMonoidPointUpdates() {
        // Non-commutative Monoid: Affine Transformation f(x) = ax + b
        // Product f2 * f1 corresponds to composition f2(f1(x)) = a2(a1*x + b1) + b2 = (a1*a2)x + (a2*b1 + b2)
        LongMonoidStrategy strategy = new LongMonoidStrategy() {
            @Override
            public long identity() {
                return pack(1, 0);
            }
            @Override
            public long mul(long f1, long f2) {
                long a1 = getA(f1);
                long b1 = getB(f1);
                long a2 = getA(f2);
                long b2 = getB(f2);
                long a = (a1 * a2) % MOD;
                long b = (a2 * b1 + b2) % MOD;
                return pack(a, b);
            }
        };

        // Path: 0 (2x + 3) -> 1 (3x + 1) -> 2 (4x + 2)
        long[] initial = { pack(2, 3), pack(3, 1), pack(4, 2) };
        LinkCutTreeMonoid lct = new LinkCutTreeMonoid(initial, strategy);

        assertTrue(lct.link(0, 1));
        assertTrue(lct.link(1, 2));

        // Path product from 0 to 2: f = f2(f1(f0(x)))
        // f0(x) = 2x + 3
        // f1(f0(x)) = 3(2x + 3) + 1 = 6x + 10
        // f2(f1(f0(x))) = 4(6x + 10) + 2 = 24x + 42
        long res = lct.pathQuery(0, 2);
        assertEquals(24, getA(res));
        assertEquals(42, getB(res));

        // Path product from 2 to 0: f = f0(f1(f2(x)))
        // f2(x) = 4x + 2
        // f1(f2(x)) = 3(4x + 2) + 1 = 12x + 7
        // f0(f1(f2(x))) = 2(12x + 7) + 3 = 24x + 17
        long revRes = lct.pathQuery(2, 0);
        assertEquals(24, getA(revRes));
        assertEquals(17, getB(revRes));

        // Update vertex 1 to 5x + 5
        lct.updateVertex(1, pack(5, 5));

        // New Path product 0 to 2: f2(f1(f0(x)))
        // f1(f0(x)) = 5(2x + 3) + 5 = 10x + 20
        // f2(f1(f0(x))) = 4(10x + 20) + 2 = 40x + 82
        res = lct.pathQuery(0, 2);
        assertEquals(40, getA(res));
        assertEquals(82, getB(res));
    }

    @Test
    public void testNonCommutativeMonoidAction() {
        // Monoid values: path sum of integers
        LongMonoidStrategy valStrategy = new LongMonoidStrategy() {
            @Override
            public long identity() {
                return 0;
            }
            @Override
            public long mul(long x, long y) {
                return (x + y) % MOD;
            }
        };

        // Lazy Action: Affine Transformation f(x) = ax + b
        // composition of actions: g(f(x)) = (a1*a2)x + (a2*b1 + b2)
        LongMonoidStrategy lazyStrategy = new LongMonoidStrategy() {
            @Override
            public long identity() {
                return pack(1, 0);
            }
            @Override
            public long mul(long f2, long f1) {
                long a1 = getA(f1);
                long b1 = getB(f1);
                long a2 = getA(f2);
                long b2 = getB(f2);
                long a = (a1 * a2) % MOD;
                long b = (a2 * b1 + b2) % MOD;
                return pack(a, b);
            }
        };

        // Monoid Action Strategy
        LongMonoidActionStrategy actionStrategy = new LongMonoidActionStrategy() {
            @Override
            public LongMonoidStrategy targetMonoidStrategy() {
                return valStrategy;
            }
            @Override
            public LongMonoidStrategy actingMonoidStrategy() {
                return lazyStrategy;
            }
            @Override
            public long act(long lz, long sum, int size) {
                long a = getA(lz);
                long b = getB(lz);
                return (a * sum + b * size) % MOD;
            }
        };

        // Path: 0 (val: 10) -> 1 (val: 20) -> 2 (val: 30)
        long[] initial = { 10, 20, 30 };
        LinkCutTreeMonoidAction lct = new LinkCutTreeMonoidAction(initial, actionStrategy);

        assertTrue(lct.link(0, 1));
        assertTrue(lct.link(1, 2));

        // Initial path sum from 0 to 2: 10 + 20 + 30 = 60
        assertEquals(60, lct.pathQuery(0, 2));

        // Apply affine transformation 2x + 5 to the path
        assertTrue(lct.pathApply(0, 2, pack(2, 5)));
        // Values become:
        // 0: 2(10) + 5 = 25
        // 1: 2(20) + 5 = 45
        // 2: 2(30) + 5 = 65
        // Sum = 25 + 45 + 65 = 135
        // Or using mapping: 2(60) + 5(3) = 120 + 15 = 135
        assertEquals(135, lct.pathQuery(0, 2));

        // Update single vertex 1 to 10
        lct.updateVertex(1, 10);
        // New values: 25, 10, 65. Sum = 100
        assertEquals(100, lct.pathQuery(0, 2));
    }

    @Test
    public void testRandomStressTestAll() {
        Random rand = new Random(1337);
        int N = 40;
        int numOperations = 1500;

        LinkCutTree bareLct = new LinkCutTree(N);
        NaiveForest bareNaive = new NaiveForest(N);

        // Monoid Point Update setup (Affine transformation)
        LongMonoidStrategy mStrategy = new LongMonoidStrategy() {
            @Override
            public long identity() {
                return pack(1, 0);
            }
            @Override
            public long mul(long f1, long f2) {
                long a1 = getA(f1);
                long b1 = getB(f1);
                long a2 = getA(f2);
                long b2 = getB(f2);
                long a = (a1 * a2) % MOD;
                long b = (a2 * b1 + b2) % MOD;
                return pack(a, b);
            }
        };
        long mIdentity = pack(1, 0);
        long[] initialM = new long[N];
        for (int i = 0; i < N; i++) initialM[i] = pack(rand.nextInt(10) + 1, rand.nextInt(10));
        LinkCutTreeMonoid mLct = new LinkCutTreeMonoid(initialM, mStrategy);
        NaiveForestMonoid mNaive = new NaiveForestMonoid(initialM, mStrategy::mul, mIdentity);

        // Monoid Action setup (Range Affine on Sum)
        LongMonoidStrategy aValStrategy = new LongMonoidStrategy() {
            @Override
            public long identity() {
                return 0;
            }
            @Override
            public long mul(long x, long y) {
                return (x + y) % MOD;
            }
        };

        LongMonoidStrategy aLazyStrategy = new LongMonoidStrategy() {
            @Override
            public long identity() {
                return pack(1, 0);
            }
            @Override
            public long mul(long f2, long f1) {
                long a1 = getA(f1);
                long b1 = getB(f1);
                long a2 = getA(f2);
                long b2 = getB(f2);
                long a = (a1 * a2) % MOD;
                long b = (a2 * b1 + b2) % MOD;
                return pack(a, b);
            }
        };

        LongMonoidActionStrategy actionStrategy = new LongMonoidActionStrategy() {
            @Override
            public LongMonoidStrategy targetMonoidStrategy() {
                return aValStrategy;
            }
            @Override
            public LongMonoidStrategy actingMonoidStrategy() {
                return aLazyStrategy;
            }
            @Override
            public long act(long lz, long sum, int size) {
                long a = getA(lz);
                long b = getB(lz);
                return (a * sum + b * size) % MOD;
            }
        };

        long[] initialA = new long[N];
        for (int i = 0; i < N; i++) initialA[i] = rand.nextInt(100);
        LinkCutTreeMonoidAction aLct = new LinkCutTreeMonoidAction(initialA, actionStrategy);
        NaiveForestAction aNaive = new NaiveForestAction(initialA, aValStrategy::mul, 0, actionStrategy::act, aLazyStrategy::mul, pack(1, 0));

        List<String> log = new ArrayList<>();

        try {
            for (int opIndex = 0; opIndex < numOperations; opIndex++) {
                int type = rand.nextInt(10);
                int u = rand.nextInt(N);
                int v = rand.nextInt(N);

                switch (type) {
                    case 0: // link
                        log.add("link(" + u + ", " + v + ")");
                        boolean bL = bareLct.link(u, v);
                        boolean bN = bareNaive.link(u, v);
                        assertEquals(bN, bL);

                        boolean mL = mLct.link(u, v);
                        boolean mN = mNaive.link(u, v);
                        assertEquals(mN, mL);

                        boolean aL = aLct.link(u, v);
                        boolean aN = aNaive.link(u, v);
                        assertEquals(aN, aL);
                        break;

                    case 1: // cut
                        log.add("cut(" + u + ", " + v + ")");
                        boolean bC = bareLct.cut(u, v);
                        boolean bNC = bareNaive.cut(u, v);
                        assertEquals(bNC, bC);

                        boolean mC = mLct.cut(u, v);
                        boolean mNC = mNaive.cut(u, v);
                        assertEquals(mNC, mC);

                        boolean aC = aLct.cut(u, v);
                        boolean aNC = aNaive.cut(u, v);
                        assertEquals(aNC, aC);
                        break;

                    case 2: // evert
                        log.add("evert(" + u + ")");
                        bareLct.evert(u);
                        bareNaive.evert(u);

                        mLct.evert(u);
                        mNaive.evert(u);

                        aLct.evert(u);
                        aNaive.evert(u);
                        break;

                    case 3: // same
                        log.add("same(" + u + ", " + v + ")");
                        assertEquals(bareNaive.same(u, v), bareLct.same(u, v));
                        assertEquals(mNaive.same(u, v), mLct.same(u, v));
                        assertEquals(aNaive.same(u, v), aLct.same(u, v));
                        break;

                    case 4: // root
                        log.add("root(" + u + ")");
                        assertEquals(bareNaive.root(u), bareLct.root(u));
                        assertEquals(mNaive.root(u), mLct.root(u));
                        assertEquals(aNaive.root(u), aLct.root(u));
                        break;

                    case 5: // lca
                        log.add("lca(" + u + ", " + v + ")");
                        assertEquals(bareNaive.lca(u, v), bareLct.lca(u, v));
                        assertEquals(mNaive.lca(u, v), mLct.lca(u, v));
                        assertEquals(aNaive.lca(u, v), aLct.lca(u, v));
                        break;

                    case 6: // kthAncestor & depth
                        int k = rand.nextInt(N + 5);
                        log.add("kthAncestor/depth(" + u + ", k=" + k + ")");
                        assertEquals(bareNaive.kthAncestor(u, k), bareLct.kthAncestor(u, k));
                        assertEquals(bareNaive.depth(u), bareLct.depth(u));

                        assertEquals(mNaive.kthAncestor(u, k), mLct.kthAncestor(u, k));
                        assertEquals(mNaive.depth(u), mLct.depth(u));

                        assertEquals(aNaive.kthAncestor(u, k), aLct.kthAncestor(u, k));
                        assertEquals(aNaive.depth(u), aLct.depth(u));
                        break;

                    case 7: // Monoid path query (Forward)
                        log.add("pathQueryM(" + u + ", " + v + ")");
                        long lctM = mLct.pathQuery(u, v);
                        long naiveM = mNaive.pathQuery(u, v);
                        assertEquals(getA(naiveM), getA(lctM));
                        assertEquals(getB(naiveM), getB(lctM));
                        break;

                    case 8: // Monoid Action path apply & query
                        long lazyVal = pack(rand.nextInt(5) + 1, rand.nextInt(10));
                        log.add("pathApplyA(" + u + ", " + v + ", lazy=" + lazyVal + ")");
                        boolean sameComp = aNaive.same(u, v);
                        assertEquals(sameComp, aLct.same(u, v));
                        if (sameComp) {
                            assertEquals(aNaive.pathApply(u, v, lazyVal), aLct.pathApply(u, v, lazyVal));
                            assertEquals(aNaive.pathQuery(u, v), aLct.pathQuery(u, v));
                        }
                        break;

                    case 9: // point update
                        long newValM = pack(rand.nextInt(10) + 1, rand.nextInt(10));
                        long newValA = rand.nextInt(100);
                        log.add("updateVertex(" + u + ", newValM=" + newValM + ", newValA=" + newValA + ")");
                        mLct.updateVertex(u, newValM);
                        mNaive.updateVertex(u, newValM);
                        assertEquals(mNaive.getVertexValue(u), mLct.getVertexValue(u));

                        aLct.updateVertex(u, newValA);
                        aNaive.updateVertex(u, newValA);
                        assertEquals(aNaive.getVertexValue(u), aLct.getVertexValue(u));
                        break;
                }
            }
        } catch (Throwable t) {
            System.out.println("Trace of operations:");
            for (String s : log) {
                System.out.println(s);
            }
            throw t;
        }
    }

    static class NaiveForest {
        int N;
        Set<Integer>[] adj;
        int[] parent;

        @SuppressWarnings("unchecked")
        NaiveForest(int N) {
            this.N = N;
            adj = new Set[N];
            for (int i = 0; i < N; i++) adj[i] = new HashSet<>();
            parent = new int[N];
            Arrays.fill(parent, -1);
        }

        List<Integer> getPath(int u, int v) {
            List<Integer> path = new ArrayList<>();
            boolean[] vis = new boolean[N];
            if (dfsPath(u, v, vis, path)) {
                return path;
            }
            return null;
        }

        private boolean dfsPath(int curr, int target, boolean[] vis, List<Integer> path) {
            vis[curr] = true;
            path.add(curr);
            if (curr == target) return true;
            for (int nxt : adj[curr]) {
                if (!vis[nxt]) {
                    if (dfsPath(nxt, target, vis, path)) return true;
                }
            }
            path.remove(path.size() - 1);
            return false;
        }

        boolean same(int u, int v) {
            return getPath(u, v) != null;
        }

        int root(int u) {
            int curr = u;
            while (parent[curr] != -1) {
                curr = parent[curr];
            }
            return curr;
        }

        void evert(int u) {
            List<Integer> path = new ArrayList<>();
            int curr = u;
            while (curr != -1) {
                path.add(curr);
                curr = parent[curr];
            }
            for (int i = 0; i < path.size() - 1; i++) {
                int childNode = path.get(i);
                int parentNode = path.get(i + 1);
                parent[parentNode] = childNode;
            }
            parent[u] = -1;
        }

        boolean link(int u, int v) {
            if (same(u, v)) return false;
            evert(u);
            parent[u] = v;
            adj[u].add(v);
            adj[v].add(u);
            return true;
        }

        boolean cut(int u, int v) {
            evert(u);
            if (!adj[u].contains(v)) return false;
            if (parent[v] == u) {
                parent[v] = -1;
            } else if (parent[u] == v) {
                parent[u] = -1;
            }
            adj[u].remove(v);
            adj[v].remove(u);
            return true;
        }

        int lca(int u, int v) {
            if (!same(u, v)) return -1;
            Set<Integer> ancestors = new HashSet<>();
            int curr = u;
            while (curr != -1) {
                ancestors.add(curr);
                curr = parent[curr];
            }
            curr = v;
            while (curr != -1) {
                if (ancestors.contains(curr)) return curr;
                curr = parent[curr];
            }
            return -1;
        }

        int depth(int u) {
            int d = 0;
            int curr = u;
            while (parent[curr] != -1) {
                d++;
                curr = parent[curr];
            }
            return d;
        }

        int kthAncestor(int u, int k) {
            if (k < 0) return -1;
            int curr = u;
            for (int i = 0; i < k; i++) {
                if (curr == -1) break;
                curr = parent[curr];
            }
            return curr;
        }
    }

    static class NaiveForestMonoid extends NaiveForest {
        long[] val;
        LongBinaryOperator op;
        long identity;

        NaiveForestMonoid(long[] initialValues, LongBinaryOperator op, long identity) {
            super(initialValues.length);
            this.val = initialValues.clone();
            this.op = op;
            this.identity = identity;
        }

        void updateVertex(int u, long newVal) {
            val[u] = newVal;
        }

        long getVertexValue(int u) {
            return val[u];
        }

        long pathQuery(int u, int v) {
            if (!same(u, v)) return identity;
            evert(u);
            List<Integer> path = getPath(u, v);
            if (path == null) return identity;
            long acc = identity;
            for (int node : path) {
                acc = op.applyAsLong(acc, val[node]);
            }
            return acc;
        }
    }

    static interface TripleMapping {
        long apply(long lazyTag, long sum, int size);
    }

    static class NaiveForestAction extends NaiveForest {
        long[] val;
        LongBinaryOperator op;
        long identity;
        TripleMapping mapping;
        LongBinaryOperator composition;
        long lazyIdentity;

        NaiveForestAction(long[] initialValues, LongBinaryOperator op, long identity, TripleMapping mapping, LongBinaryOperator composition, long lazyIdentity) {
            super(initialValues.length);
            this.val = initialValues.clone();
            this.op = op;
            this.identity = identity;
            this.mapping = mapping;
            this.composition = composition;
            this.lazyIdentity = lazyIdentity;
        }

        void updateVertex(int u, long newVal) {
            val[u] = newVal;
        }

        long getVertexValue(int u) {
            return val[u];
        }

        long pathQuery(int u, int v) {
            if (!same(u, v)) return identity;
            evert(u);
            List<Integer> path = getPath(u, v);
            if (path == null) return identity;
            long acc = identity;
            for (int node : path) {
                acc = op.applyAsLong(acc, val[node]);
            }
            return acc;
        }

        boolean pathApply(int u, int v, long lazyVal) {
            if (!same(u, v)) return false;
            evert(u);
            List<Integer> path = getPath(u, v);
            if (path == null) return false;
            for (int node : path) {
                val[node] = mapping.apply(lazyVal, val[node], 1);
            }
            return true;
        }
    }
}