import java.util.List;
import java.util.Random;

/**
 * Chimp Optimization Algorithm (ChOA) untuk penjadwalan task.
 *
 * Referensi: Khishe, M., & Mosavi, M. R. (2020). Chimp optimization algorithm.
 * Expert Systems with Applications, 149, 113338.
 * https://doi.org/10.1016/j.eswa.2020.113338
 *
 * Empat chimp terbaik menjadi pemimpin: attacker, barrier, chaser, driver.
 * Setiap chimp lain bergerak mengikuti keempatnya (Persamaan 6 sampai 8 paper):
 *   d  = | c * x_pemimpin - m * x |
 *   xk = x_pemimpin - a * d                 (untuk tiap pemimpin)
 *   x_baru = (x1 + x2 + x3 + x4) / 4
 * dengan
 *   f  turun linear dari 2,5 ke 0 selama iterasi
 *   a  = 2 * f * r1 - f
 *   c  = 2 * r2
 *   m  = nilai chaotic map (Persamaan 5). Di sini dipakai logistic map.
 *
 * Catatan: pemimpin disimpan lintas iterasi dan hanya diganti kalau ada chimp
 * yang lebih baik, sehingga solusi terbaik tidak pernah hilang.
 *
 * Populasi awal acak. Kalau Config.INIT_WITH_OLB = true, satu agen diisi solusi OLB.
 */
public class ChoaScheduler implements Scheduler {

    private final int popSize;
    private final int maxIter;

    public ChoaScheduler(int popSize, int maxIter) {
        this.popSize = popSize;
        this.maxIter = maxIter;
    }

    @Override
    public String name() {
        return "CHOA";
    }

    @Override
    public Result schedule(long[] taskLengths, List<VmSpec> vms, long seed) {
        Random rng = new Random(seed);
        Fitness fit = new Fitness(taskLengths, vms);
        int dim = fit.dim();
        double lb = fit.lb, ub = fit.ub;

        // 4 pemimpin: 0 = attacker, 1 = barrier, 2 = chaser, 3 = driver
        double[][] leader = new double[4][dim];
        double[] leaderFit = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};

        // ---------- Inisialisasi populasi acak ----------
        double[][] pos = new double[popSize][dim];
        for (int i = 0; i < popSize; i++) {
            for (int d = 0; d < dim; d++) {
                pos[i][d] = lb + rng.nextDouble() * (ub - lb);
            }
            if (i == 0 && Config.INIT_WITH_OLB) {
                // inisialisasi hibrida: agen pertama = solusi OLB
                pos[i] = fit.encode(new OlbScheduler().schedule(taskLengths, vms, seed).assignment(), rng);
            }
            updateLeaders(pos[i], fit.evaluate(pos[i]), leader, leaderFit);
        }

        double chaos = 0.7; // nilai awal logistic map
        double[] convergence = new double[maxIter + 1];
        convergence[0] = leaderFit[0];

        for (int t = 1; t <= maxIter; t++) {
            double fCoef = 2.5 - t * (2.5 / maxIter);

            for (int i = 0; i < popSize; i++) {
                for (int d = 0; d < dim; d++) {
                    double sum = 0.0;
                    for (int k = 0; k < 4; k++) {
                        double a = 2 * fCoef * rng.nextDouble() - fCoef;
                        double c = 2 * rng.nextDouble();
                        chaos = 4.0 * chaos * (1.0 - chaos); // logistic map
                        if (chaos < 1e-9 || chaos > 1 - 1e-9 || chaos == 0.75) {
                            chaos = 0.1 + 0.8 * rng.nextDouble(); // cegah macet di titik tetap
                        }
                        double dist = Math.abs(c * leader[k][d] - chaos * pos[i][d]);
                        sum += leader[k][d] - a * dist;
                    }
                    pos[i][d] = fit.clamp(sum / 4.0);
                }
            }

            for (int i = 0; i < popSize; i++) {
                updateLeaders(pos[i], fit.evaluate(pos[i]), leader, leaderFit);
            }
            convergence[t] = leaderFit[0];
        }

        return new Result(fit.decode(leader[0]), leaderFit[0], convergence, fit.nfe());
    }

    /** Sisipkan chimp ke daftar 4 pemimpin kalau fitness-nya cukup baik. */
    private static void updateLeaders(double[] x, double fx, double[][] leader, double[] leaderFit) {
        for (int k = 0; k < 4; k++) {
            if (fx < leaderFit[k]) {
                for (int s = 3; s > k; s--) {
                    leaderFit[s] = leaderFit[s - 1];
                    System.arraycopy(leader[s - 1], 0, leader[s], 0, x.length);
                }
                leaderFit[k] = fx;
                System.arraycopy(x, 0, leader[k], 0, x.length);
                return;
            }
        }
    }
}
