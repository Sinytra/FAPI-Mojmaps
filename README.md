# The Fabric API on Mojang mappings (sorta)

The main branch of this repository contains the neccessary scripts for remapping Fabric API sources to Mojang mappings.
Other branches include:

- `fabric/<version>` - upstream tracking branches, used to keep track of our reference point for mapped code
- `mojmap/<version>` - remapped Fabric API sources

Please note that to avoid possible merge conflicts, no build system changes have been made to mojmap mapped sources.
Only `java` and `accesstransformer` source files have been modified.

This repository acts as an upstream remote for the [ForgifiedFabricAPI](https://github.com/Sinytra/ForgifiedFabricAPI),
allowing us to easily update it via git rebasing.

### Usage guide

Please make sure the `kotlin` [command-line compiler](https://kotlinlang.org/docs/command-line.html) is installed on
your system, as it is necessary to run our setup scripts.

After cloning the repository, **before importing the gradle project**, run `kotlin scripts/setup.main.kts`. This will
initialize a git submodule for upstream Fabric API, which serves as an input for our remapper.

To sync the mapped sources with upstream, run `kotlin scripts/sync-upstream.main.kts`, which will automatically run
the necessary gradle tasks and perform a few git operations. As updating is done on a per-commit basis, this may take
several minutes to complete based on how far behind we are.
If a mapped branch doesn't exist yet, an initial mapping commit will be created (using your configured committer
information). Once remapping is complete, all changes are pushed from the submodule to the root repository, from where
they can be pushed to `origin`.

### Credits

- [Architectury Loom](https://github.com/architectury/architectury-loom) for allowing us to run a modding environment
  with both Yarn and Mojang mappings available
- [Cadix Dev Team](https://github.com/CadixDev) for their awesome Java source remapping
  tools - [Mercury](https://github.com/CadixDev/Mercury) and [MercuryMixin](https://github.com/CadixDev/MercuryMixin)