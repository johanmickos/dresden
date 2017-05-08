package dresden.sim.tests.crdt;

import dresden.sim.ScenarioGen;
import dresden.sim.ScenarioSetup;
import dresden.sim.TestBase;
import org.junit.Test;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class GSetTest extends TestBase {
    @Test
    public void basic() {
        int numNodes = 2;
        SimulationScenario.setSeed(ScenarioSetup.scenarioSeed);
        SimulationScenario simpleBootScenario = ScenarioGen.gsetNoChurn(numNodes);
        simpleBootScenario.simulate(LauncherComp.class);

        Set<String> prev = null;
        for (String key : res.keys()) {
            Set<String> it = res.get(key, Set.class);
            if (prev == null) {
                prev = it;
            } else {
                assertTrue(prev.containsAll(it));
            }
        }
    }
}
