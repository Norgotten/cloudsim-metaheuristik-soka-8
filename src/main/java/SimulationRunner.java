import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Menjalankan SATU simulasi CloudSim dengan pembagian task yang sudah diputuskan
 * oleh scheduler, lalu menghitung 4 metrik.
 *
 * Alur CloudSim singkatnya:
 *   CloudSim (mesin simulasi) -> Datacenter -> Host (server fisik) -> VM
 *   Broker = "kurir" yang menyerahkan VM dan task (cloudlet) ke datacenter.
 *   Keputusan task ke VM mana sudah dibuat scheduler, broker tinggal mengantar.
 */
public final class SimulationRunner {

    private SimulationRunner() {}

    public record VmStat(int vmId, String type, int taskCount, double busyTime) {}

    public record Metrics(double makespan, double totalCost, double resourceUtilization,
                          double degreeOfImbalance, List<VmStat> vmStats) {}

    public static Metrics run(long[] taskLengths, int[] assignment, List<VmSpec> specs) {
        CloudSim simulation = new CloudSim();

        new DatacenterSimple(simulation, createHosts(), new GradedVmAllocationPolicy());
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        List<Vm> vms = new ArrayList<>();
        for (VmSpec s : specs) {
            Vm vm = new VmSimple(s.id(), s.mipsPerPe(), s.pes())
                    .setRam(s.ramMb())
                    .setBw(1000)
                    .setSize(10000)
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vm.setDescription(s.type());
            vms.add(vm);
        }
        broker.submitVmList(vms);

        List<Cloudlet> cloudlets = new ArrayList<>();
        for (int i = 0; i < taskLengths.length; i++) {
            Cloudlet c = new CloudletSimple(i, taskLengths[i], 1)
                    .setFileSize(300)
                    .setOutputSize(300)
                    .setUtilizationModelCpu(new UtilizationModelFull());
            c.setVm(vms.get(assignment[i]));
            cloudlets.add(c);
        }
        broker.submitCloudletList(cloudlets);

        simulation.start();

        List<Cloudlet> finished = broker.getCloudletFinishedList();
        if (finished.size() != taskLengths.length) {
            throw new IllegalStateException("Hanya " + finished.size() + " dari "
                    + taskLengths.length + " task yang selesai");
        }
        return computeMetrics(finished, specs);
    }

    /**
     * Rumus metrik:
     *  Makespan             = waktu selesai task terakhir - waktu mulai task pertama
     *  Waktu sibuk VM j     = jumlah waktu eksekusi task di VM j / jumlah core VM j
     *                         (VM LARGE punya 2 core, jadi 2 task bisa jalan bersamaan)
     *  Total Cost           = jumlah (waktu eksekusi task x tarif VM yang menjalankannya)
     *  Resource Utilization = jumlah waktu sibuk semua VM / (jumlah VM x makespan) x 100%
     *  Degree of Imbalance  = (sibuk terlama - sibuk tersingkat) / rata-rata sibuk
     */
    private static Metrics computeMetrics(List<Cloudlet> finished, List<VmSpec> specs) {
        int m = specs.size();
        double[] cpuTime = new double[m];
        int[] count = new int[m];
        double totalCost = 0.0;
        double firstStart = Double.POSITIVE_INFINITY;
        double lastFinish = 0.0;

        for (Cloudlet c : finished) {
            int vmId = (int) c.getVm().getId();
            double exec = c.getActualCpuTime();
            cpuTime[vmId] += exec;
            count[vmId]++;
            totalCost += exec * specs.get(vmId).costPerSecond();
            firstStart = Math.min(firstStart, c.getExecStartTime());
            lastFinish = Math.max(lastFinish, c.getFinishTime());
        }

        double makespan = lastFinish - firstStart;

        List<VmStat> stats = new ArrayList<>();
        double sumBusy = 0, maxBusy = 0, minBusy = Double.POSITIVE_INFINITY;
        for (int j = 0; j < m; j++) {
            double busy = cpuTime[j] / specs.get(j).pes();
            stats.add(new VmStat(j, specs.get(j).type(), count[j], busy));
            sumBusy += busy;
            maxBusy = Math.max(maxBusy, busy);
            minBusy = Math.min(minBusy, busy);
        }
        double avgBusy = sumBusy / m;
        double ru = sumBusy / (m * makespan) * 100.0;
        double di = (maxBusy - minBusy) / avgBusy;

        return new Metrics(makespan, totalCost, ru, di, stats);
    }

    /** 2 host High Performance (4 core x 2.000 MIPS) + 2 host Standard (4 core x 1.000 MIPS). */
    private static List<Host> createHosts() {
        List<Host> list = new ArrayList<>();
        for (int i = 0; i < 2; i++) list.add(host(2000, 16384));
        for (int i = 0; i < 2; i++) list.add(host(1000, 8192));
        return list;
    }

    private static Host host(double mipsPerCore, long ramMb) {
        List<Pe> pes = new ArrayList<>();
        for (int p = 0; p < 4; p++) pes.add(new PeSimple(mipsPerCore));
        return new HostSimple(ramMb, 10000, 1000000, pes);
    }
}
