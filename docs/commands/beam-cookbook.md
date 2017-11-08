# Init

The `beam init` command is used to initialize a new project from a package of Beam configurations. 

Beam packages can also contain configuration from other automation tools such as Packer and Chef.

## Usage

Usage: `beam init <package source>`

## Source

The `package source` is the only required argument. This tells Beam where to download the package.

Currently supported values for package source are:

- Local file paths
- Github
- Git

### Github

Beam will automatically translate short package sources as github repositories. Example:

```
beam init perfectsense/beam-brightspot
```

Github repositories require that Git is installed on your system. 

### Git

Beam also supports Git repositories using a the ssh protocol.

```
beam init git@github.com:perfectsense/beam-brightspot.git
```

Git repositories require that Git is installed on your system. 