package yeene.kallisto;

import org.fest.assertions.Condition;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import yeene.kallisto.math.Vector;
import yeene.kallisto.systembuilder.SystemBuilder;

import java.math.BigDecimal;

import static org.fest.assertions.Assertions.assertThat;
import static yeene.kallisto.math.Vector.NULLVECTOR;

/**
 * @author yeene
 */
public class SimulatedSystemTest {

  public static final Vector INITIAL_POSITION_PLANET_1 = new Vector(100.0, 0.0, 0.0);
  public static final Vector INITIAL_POSITION_PLANET_2 = new Vector(0.0, 0.0, 0.0);
  public static final Vector INITIAL_POSITION_PLANET_3 = new Vector(0.0, 100.0, 0.0);
  public static final int MAXIMUM_STEPS_BEFORE_COLLISION_IS_EXPECTED = 100000;

  private SimulatedSystem simulatedSystem;

  @BeforeMethod
  public void setUp() throws Exception {
    simulatedSystem = new SimulatedSystem();
  }

  @Test
  public void step_increasesIterationCount() throws Exception {
    // fixture: at first: iteration count is 0

    // execution: call step three times, iteration count must go up each time.
    assertThat(simulatedSystem.getIterationCount()). describedAs("number of iterations").isEqualTo(0);
    simulatedSystem.step();
    assertThat(simulatedSystem.getIterationCount()). describedAs("number of iterations").isEqualTo(1);
    simulatedSystem.step();
    assertThat(simulatedSystem.getIterationCount()). describedAs("number of iterations").isEqualTo(2);

    // assertion:
  }

  @Test
  public void step_twoPlanetsFallNearEachOther_whenTheyHaveNoInitialVelocity() throws Exception {
    // fixture setup: make a simulatedSystem of two planets.
    final Sattelite planet1 = generateFirstPlanet();
    final Sattelite planet2 = generateSecondPlanet();

    simulatedSystem.addPlanets(planet1, planet2);

    // execution: step one bit.
    simulatedSystem.step();

    // assertion: the distance should be smaller after step than before step.
    final Vector distanceAfterStep = planet1.getPosition().sub(planet2.getPosition());
    final Vector distanceBeforeStep = INITIAL_POSITION_PLANET_1.sub(INITIAL_POSITION_PLANET_2);
    final BigDecimal differenceInDistance = distanceBeforeStep.sub(distanceAfterStep).length();

    assertThat(differenceInDistance.compareTo(BigDecimal.ZERO)).
      describedAs("distance between the planets after one step").
      isEqualTo(1); // >0
  }

  @Test
  public void step_hasSymmetricalResult_whenOneCentralBodyIsPulledBetweenTwoOthers() throws Exception {
    // fixture:
    final Sattelite s1 = generateFirstPlanet();
    final Sattelite s2 = generateSecondPlanet();
    final Sattelite s3 = generateThirdPlanet();
    simulatedSystem.addPlanets(s1, s2, s3);

    // execution: perform one step
    simulatedSystem.step();

    // assertion: planet 2 should have same x any y value and both should have gone in the direction of (100.0, 100.0)
    assertThat(s2.getPosition().getX()).
      describedAs("x position of second planet").
      isEqualTo(s2.getPosition().getY());

    assertThat(s1.getPosition().getX()).
      describedAs("x position of first planet").
      isEqualTo(s3.getPosition().getY());

    assertThat(s1.getPosition().getY()).
      describedAs("y position of first planet").
      isEqualTo(s3.getPosition().getX());
  }

  @Test(dataProvider = "number of steps")
  public void step_rotationalImpulseAfterStepIsTheSameAsBeforeStep(final int numberOfSteps) throws Exception {
    // fixture setup: make a simulated System of two objects and note their individual rotational impulse.
    simulatedSystem = new SystemBuilder() {{
      createObject().named("sun").withRadius(1392700000l).withMass(1.989E30).withPosition(NULLVECTOR);
      createObject().named("mercury").withRadius(2439000l).withMass(3.302E23).withEclipticInclination(7.0).withBigHalfAxis(57909000000l).withThetaInDegrees(90).withStartSpeed(47870l);
    }}.getSystem();

    final Sattelite planet1 = simulatedSystem.getElements().get(0);
    final Sattelite planet2 = simulatedSystem.getElements().get(1);

    final Vector centerOfMassBefore = getCenterOfMass(simulatedSystem);
    final Vector rotationalImpulsePlanet1Before = rotationalImpulse(planet1, centerOfMassBefore);
    final Vector rotationalImpulsePlanet2Before = rotationalImpulse(planet2, centerOfMassBefore);

    // execution: perform a step.
    for(int i=0;i<numberOfSteps;i++) {
      simulatedSystem.step();
    }


    // assertion: get impulse after stepping and compare to original.
    final Vector centerOfMassAfter = getCenterOfMass(simulatedSystem);
    final Vector rotationalImpulsePlanet1After = rotationalImpulse(planet1, centerOfMassAfter);
    final Vector rotationalImpulsePlanet2After = rotationalImpulse(planet2, centerOfMassAfter);

    final Vector totalRotationalImpulseBefore = rotationalImpulsePlanet1Before.add(rotationalImpulsePlanet2Before);
    final Vector totalRotationalImpulseAfter = rotationalImpulsePlanet1After.add(rotationalImpulsePlanet2After);

    assertThat(totalRotationalImpulseBefore.length().subtract(totalRotationalImpulseAfter.length())).
      describedAs("impulse change on step for planet 1").
      isZero();
  }

  private Vector rotationalImpulse(final Sattelite planet, final Vector centerOfMass) {
    final Vector impulse = planet.getVelocity().mult(planet.getMass());
    final Vector radius = planet.getPosition().sub(centerOfMass);

    return impulse.crossProduct(radius);
  }

  private Vector getCenterOfMass(final SimulatedSystem simulatedSystem) {
    Vector result = Vector.NULLVECTOR;
    BigDecimal totalMass = BigDecimal.ZERO;

    for(final Sattelite s : simulatedSystem.getElements()) {
      result = result.add(s.getPosition().mult(s.getMass()));
      totalMass = totalMass.add(s.getMass());
    }

    return result.div(totalMass);
  }

  @DataProvider(name = "number of steps")
  public Object[][] numberOfStepsProvider() {
    return new Object[][] {
      new Object[] {      1 },
      new Object[] {     10 },
      new Object[] {    100 },
      new Object[] {   1000 },
      new Object[] {  10000 },
      new Object[] { 100000 },
    };
  }

  @Test
  public void step_twoPlanetsCollide_whenTheyHaveNoInitialVelocity() throws Exception {
    // fixture setup: make a simulatedSystem of two planets
    final Sattelite planet1 = generateFirstPlanet();
    final Sattelite planet2 = generateSecondPlanet();

    simulatedSystem.addPlanets(planet1, planet2);

    // execution: execute for a while, wait until the planets distance increases. Should happen within {@link MAXIMUM_STEPS_BEFORE_COLLISION_IS_EXPECTED} steps.
    BigDecimal lastDistance = INITIAL_POSITION_PLANET_1.sub(INITIAL_POSITION_PLANET_2).length();
    int i=0;
    for (; i < MAXIMUM_STEPS_BEFORE_COLLISION_IS_EXPECTED; i++) {
      simulatedSystem.step();

      final BigDecimal newDistance = planet1.getPosition().sub(planet2.getPosition()).length();
      if(lastDistance.compareTo(newDistance) < 0) {
        // distance has increased again. planets have passed each other in the simmulation.
        break;
      }

      lastDistance = newDistance;
    }

    // assertion: should have taken less than 100000 steps to collide.
    assertThat(i).
      describedAs("numebr of steps before collision").
      satisfies(new Condition<Integer>() {
        @Override
        public boolean matches(final Integer value) {
          return value < MAXIMUM_STEPS_BEFORE_COLLISION_IS_EXPECTED;
        }
      });
  }

  private Sattelite generateFirstPlanet() {
    return new Sattelite("Planet 1", BigDecimal.TEN, BigDecimal.valueOf(8000000000000l), INITIAL_POSITION_PLANET_1, Vector.NULLVECTOR, Vector.NULLVECTOR);
  }

  private Sattelite generateMovingFirstPlanet() {
    return new Sattelite("Planet 1", BigDecimal.TEN, BigDecimal.valueOf(8000000000000l), INITIAL_POSITION_PLANET_1, new Vector(0.0, 10.0, 0.0), Vector.NULLVECTOR);
  }

  private Sattelite generateSecondPlanet() {
    return new Sattelite("Planet 2", BigDecimal.TEN, BigDecimal.valueOf(300000000000l), INITIAL_POSITION_PLANET_2, Vector.NULLVECTOR, Vector.NULLVECTOR);
  }

  private Sattelite generateMovingSecondPlanet() {
    return new Sattelite("Planet 2", BigDecimal.TEN, BigDecimal.valueOf(300000000000l), INITIAL_POSITION_PLANET_2, new Vector(-10.0, 0.0, 0.0), Vector.NULLVECTOR);
  }

  private Sattelite generateThirdPlanet() {
    return new Sattelite("Planet 3", BigDecimal.TEN, BigDecimal.valueOf(8000000000000l), INITIAL_POSITION_PLANET_3, Vector.NULLVECTOR, Vector.NULLVECTOR);
  }

}
