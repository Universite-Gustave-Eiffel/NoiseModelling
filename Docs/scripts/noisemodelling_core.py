import jnius_config
import os
import time

deb = time.time()
jnius_config.add_options('-Xmx4096m')
jnius_config.set_classpath('scriptrunner/lib/*')

from jnius import autoclass

# Imports
RoadSourceParametersCnossos = autoclass('org.noise_planet.noisemodelling.emission.RoadSourceParametersCnossos')
EvaluateRoadSourceCnossos = autoclass('org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos')
CnossosPropagationData = autoclass('org.noise_planet.noisemodelling.pathfinder.CnossosPropagationData')
runnerMain = autoclass('org.noisemodelling.runner.Main')
LoggerFactory = autoclass('org.slf4j.LoggerFactory')
ComputeCnossosRays = autoclass('org.noise_planet.noisemodelling.pathfinder.ComputeCnossosRays')
IComputeRaysOut = autoclass('org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut')
ProfileBuilder = autoclass('org.noise_planet.noisemodelling.pathfinder.ProfileBuilder')
ProfilerThread = autoclass('org.noise_planet.noisemodelling.pathfinder.utils.ProfilerThread')
ComputeRaysOutAttenuation = autoclass('org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation')
PropagationProcessPathData = autoclass('org.noise_planet.noisemodelling.propagation.PropagationProcessPathData')
Coordinate = autoclass('org.locationtech.jts.geom.Coordinate')
Array = autoclass('java.lang.reflect.Array')

def T02():
    lv_speed = 70
    lv_per_hour = 1000
    mv_speed = 70
    mv_per_hour = 1000
    hgv_speed = 70
    hgv_per_hour = 1000
    wav_speed = 70
    wav_per_hour = 1000
    wbv_speed = 70
    wbv_per_hour = 1000
    FreqParam = 8000
    Temperature = 15
    RoadSurface = "NL01"
    Pm_stud = 0.5
    Ts_stud = 4
    Junc_dist = 200
    Junc_type = 1
    rsParameters = RoadSourceParametersCnossos(lv_speed, mv_speed, hgv_speed, wav_speed, wbv_speed, lv_per_hour,
                                               mv_per_hour, hgv_per_hour, wav_per_hour, wbv_per_hour, FreqParam,
                                               Temperature, RoadSurface, Ts_stud, Pm_stud, Junc_dist, Junc_type)
    rsParameters.setSlopePercentage_without_limit(10)
    rsParameters.setCoeffVer(1)
    return EvaluateRoadSourceCnossos.evaluate(rsParameters)


def to_coordinate_array(coordinate_list):
    ar = Array.newInstance(Coordinate, len(coordinate_list))
    for i, v in enumerate(coordinate_list):
        ar[i] = Coordinate(v[0], v[1], v[2])
    return ar

def propagationDemo():
    # use default env data 15°C 70% humidity 101325 Pa (octave)
    defaultEnvironmentalData = PropagationProcessPathData()
    # profile builder is a class that help computing intersection with 3d objects such as buildings,
    # digital terrain model, ground type areas
    builder = ProfileBuilder()
    # Add building addWall or addBuilding
    builder.addBuilding(to_coordinate_array([]))
    threadData = CnossosPropagationData(builder, defaultEnvironmentalData.freq_lvl)
    computeRays = ComputeCnossosRays(threadData)


def main():
    logger = LoggerFactory.getLogger("mainlog")
    runnerMain.printBuildIdentifiers(logger)
    sourceNoiseLevel = T02()  # source noise level in dB(A)


if __name__ == '__main__':
    main()
    print("Done in %s " % (time.strftime("%H:%M:%S", time.gmtime(time.time() - deb))))
