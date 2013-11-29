Noisemap
======

This OrbisGIS plugin-in is a simplified approach of the [NMPB 2008][nmpb]. The processing is done through SQL request with the following sql functions:

* BR_EvalSource Return the dB(A) value corresponding to light and heavy vehicle traffic parameters;
* BR_SpectrumRepartition Return the frequency band level for a light and heavy vehicle;
* BTW_EvalSource Return the dB(A) value corresponding to tramway traffic parameters;
* BTW_SpectrumRepartition Return the frequency band level for a tramway;
*	BR_TriGrid Return noise level on an irregular grid, using building and noise source as input;

Starting from the triangular grid you can use OrbisGIS's interpolation to make final noise map.

This plugin can handle unlimited area (use case of 523km²) take advantage of the latest multi-core processors. It can be run on Windows,MacOS and linux as it is fully written in Java.

For more information [read the wiki] (https://github.com/irstv/noisemap/wiki).

[nmpb]: http://www.setra.equipement.gouv.fr/IMG/pdf/US_0957-2A_Road_noise_predictionDTRF.pdf "F. Besnard, J. Defrance, M. Bérengier, G. Dutilleux,F. Junker, D. Ecotiere, E. Le Duc, M. Baulac, B.Bonhomme, J.-P. Deparis, B. Gauvreau, V. Guizard,H. Lefèvre, V. Steimer, D. Van Maercke, V. Zoubo#,'Road noise prediction: 2 - Noise propagation computation method including meteorological effects (NMPB 2008)', SETRA (2009)"
