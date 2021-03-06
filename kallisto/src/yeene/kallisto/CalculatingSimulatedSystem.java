package yeene.kallisto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simulation of a System of n bodies.
 *
 * This is the base class for the Simulation. It's purpose is holding the objects and performing
 * the calculation iterations for each step.
 *
 * @author yeene
 */
public class CalculatingSimulatedSystem implements SimulatedSystem {

  private List<Satellite> bodies = new ArrayList<Satellite>();
  private long numberOfIterationSteps = 0l;

  /**
   * This Method lets the system go to the next state.
   * It will calculate the forces the bodies in the simulation affect each other with and then
   * apply the changes in position, velocity etc. to them.
   */
  @Override
  public void step() {
    final List<SatelliteBodiesStateChangeCollector> stateChanges = new ArrayList<SatelliteBodiesStateChangeCollector>();

    // go through all planets and calculate the differences.
    for(final Satellite i : bodies) {

      // get the base object
      SatelliteBodiesStateChangeCollector sw = new SatelliteBodiesStateChangeCollector(i);
      stateChanges.add(sw);

      // calculate other object's influence on this object.
      for(final Satellite j : bodies) {
        sw.influenceSatellite(j);
      }
    }

    // step through the satellites once more and perform calculations on the
    // objects.
    for(final SatelliteBodiesStateChangeCollector collector : stateChanges) {
      collector.apply();
    }

    numberOfIterationSteps++;
  }

  /**
   * Add one or more Satellites to the system.
   *
   * @param planets one or multiple objects for the simulation
   */
  @Override
  public void addPlanets(final Satellite... planets) {
    Collections.addAll(bodies, planets);
  }

  /**
   * @return the elements in the system.
   */
  @Override
  public List<Satellite> getElements() {
    return bodies;
  }

  /**
   * @return the number of iteration steps in this simulation.
   */
  @Override
  public long getIterationCount() {
    return numberOfIterationSteps;
  }

  /**
   * find an object by it's given name.
   * @param name name of the object to find.
   * @return the object found or null if not found.
   */
  @Override
  public Satellite find(final String name) {
    for(final Satellite s : bodies) {
      if(s.getName().equals(name)) {
        return s;
      }
    }

    return null;
  }


  /**
   * Convenience Method for geting a bounding box around the stuff
   * @return a bounding box around the system
   */
  @Override
  public BoundingBox getBoundingBox() {
    BigDecimal maxx = new BigDecimal("-99999999999999999999999");
    BigDecimal maxy = new BigDecimal("-99999999999999999999999");
    BigDecimal maxz = new BigDecimal("-99999999999999999999999");
    BigDecimal minx = new BigDecimal("99999999999999999999999");
    BigDecimal miny = new BigDecimal("99999999999999999999999");
    BigDecimal minz = new BigDecimal("99999999999999999999999");

    for(final Satellite s : bodies) {
      maxx = s.getPosition().getX().compareTo(maxx) > 0 ? s.getPosition().getX() : maxx;
      maxy = s.getPosition().getY().compareTo(maxy) > 0 ? s.getPosition().getY() : maxy;
      maxz = s.getPosition().getZ().compareTo(maxz) > 0 ? s.getPosition().getZ() : maxz;

      minx = s.getPosition().getX().compareTo(minx) < 0 ? s.getPosition().getX() : minx;
      miny = s.getPosition().getY().compareTo(miny) < 0 ? s.getPosition().getY() : miny;
      minz = s.getPosition().getZ().compareTo(minz) < 0 ? s.getPosition().getZ() : minz;
    }

    return new BoundingBox(maxx, maxy, maxz, minx, miny, minz);
  }
}
