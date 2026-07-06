# Contributing to NoiseModelling

First off, thanks for taking the time to contribute!

The following is a set of guidelines for contributing to NoiseModelling. These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.


## Requirements

To contribute (code, patches, or documentation) to the NoiseModelling project, all developers/team members must:

- Abide by the [Social Contract](#social-contract).
- Be familiar with the development process and the reference material available in NoiseModelling Sources.
- Ensure that all code committed to the repository is unencumbered and compatible with the [NoiseModelling license](LICENSE.md), GNU Public License (GPL) Version 3 or any later version.
- Not use or include any code that will cause a trademark or patent infringement. If you are unsure, ask on a Project Steering Committee member.
- These requirements apply to both developers desiring commit privileges and those that submit patches.
- Read and agree to the [Code of Conduct](CODE_OF_CONDUCT.md).


## Social contract

NoiseModelling is a collaborative project. As an open source project we hope for and  encourage participation and code contributions from interested parties.  However in order to maintain high standards we encourage you to  participate in the project according to the following social contract:

- To be polite to all members of the project
- To ensure any code you submit via a patch or commit to source control is properly documented
- To ensure any code you submit via a patch or commit to source control is compliant with our CodingStandards
- Not to submit any code without having permission to submit that code
- Not to submit any code that violates the GNU Public License
- To follow the [Contributor Requirements](#requirements) mentioned above
- To ensure (to the best of your ability) that any commits you make to Git leave the repository in a compilable state
- To ensure that any new class added to the source is accompanied by a test unit
- To carry out appropriate tests before committing new code to the repository


## Code of Conduct

This project and everyone participating in it is governed by the [NoiseModelling Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.


## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report for NoiseModelling. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

*   **Check the [issues](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/issues)** to see if the problem has already been reported.
*   **Open a new issue**. Explain the problem and include additional details to help maintainers reproduce the problem:
    *   Use a clear and descriptive title for the issue to identify the problem.
    *   Describe the exact steps to reproduce the problem in as many details as possible.
    *   Describe the behavior you observed after following the steps and point out what exactly is the problem with that behavior.
    *   Explain which behavior you expected to see instead and why.
    *   Include screenshots and animated GIFs which show you following the reproduction steps.

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for NoiseModelling, including completely new features and minor improvements to existing functionality.

*   **Check the [issues](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/issues) and [discussions](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/discussions)** to see if the enhancement has already been suggested.
*   **Open a new issue** or start a **new discussion**. Describe the enhancement and provide as much detail as possible.

### Your First Code Contribution

Unsure where to begin contributing to NoiseModelling? You can start by looking through these `good first issue` and `help wanted` issues:

*   [Good first issues](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/labels/good%20first%20issue) - issues which should only require a few lines of code, and a test or two.
*   [Help wanted issues](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/labels/help%20wanted) - issues which should be a bit more involved than `good first issue`.

### Pull Requests

*   **Fork the repository** and clone it locally.
*   Create a branch for your edits.
*   Make sure your code lints and tests pass.
*   **Submit a pull request** to the `main` branch, using `--signoff` parameter (see [Authentication](#Authentication) below)
*   If you're adding a new feature, add tests for it.


#### Authentication

As with many open and collaborative projects, it is necessary to clarify the issue of contributor authentication.
One solution would be to have each contributor sign a [Contributor Licence Agreement](https://en.wikipedia.org/wiki/Contributor_license_agreement) (CLA). However, we consider this solution to be somewhat restrictive.
We have therefore opted for a simpler solution: the [Developer Certificate of Origin](https://en.wikipedia.org/wiki/Developer_Certificate_of_Origin) (DCO). 

In practical terms, when pushing their code, each contributor must sign their contribution using the [Git command: --signoff](https://git-scm.com/docs/git-commit#Documentation/git-commit.txt---signoff)

Example :

`nm_contributor@my_pc:~$ git commit -m "Description of my contrib" --signoff`

If the signature is not linked to the push, we will not be able to accept the contribution as it stands.

## Styleguides

### Java or Groovy Styleguide

*   Please follow the existing coding style in the project.
*   Ensure that your code is well-documented (e.g with Javadoc) where appropriate.

### Documentation Styleguide

*   Documentation is written in reStructuredText (.rst) and Markdown (.md).
*   Correct spelling and grammar are appreciated.

## Additional Resources

*   [Get Started Dev Documentation](https://noisemodelling.readthedocs.io/en/latest/Get_Started_Dev.html)
*   [NoiseModelling Website](http://noise-planet.org/noisemodelling.html)


Note: This document is partly based on the great [QGIS](https://qgis.org) project, which has set out very clearly the elements we wished to apply to NoiseModelling ([see](https://qgis.org/resources/support/contributors-requirements/)).
