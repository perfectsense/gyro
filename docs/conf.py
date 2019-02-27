import sys
import os
import bsp_docs_sphinx_theme

# -- General configuration ------------------------------------------------

# If your documentation needs a minimal Sphinx version, state it here.
needs_sphinx = '1.5.2'

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
    'sphinx.ext.intersphinx',
    'sphinx.ext.todo',
    'sphinxprettysearchresults',
    'rst2pdf.pdfbuilder',
    'recommonmark'
]

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

source_suffix = ['.rst', '.txt']
master_doc = 'index'

project = u'Beam'
copyright = u'2017, Perfect Sense'
author = u'Perfect Sense'
release = u'1.0'
language = None

exclude_patterns = [
    '_build', 
    'Thumbs.db', 
    '.DS_Store', 
    'node_modules',
    'requirements.txt',
    'training',
    'inbox',
    'demo',
    'html'
]

# The name of the Pygments (syntax highlighting) style to use.
pygments_style = 'sphinx'

html_theme = 'sphinx_rtd_theme'
html_theme_options = {
    'collapse_navigation': False,
    'display_version': False,
    'navigation_depth': 3,
}

html_theme_path = [bsp_docs_sphinx_theme.get_html_theme_path()]
html_static_path = ['_static']

rst_prolog = """
.. include:: /substitutions.tsr
"""

html_show_sourcelink = False
html_show_sphinx = False

todo_include_todos = False

pdf_documents = [
 ('gyro/index',
     u'Beam',
     u'Beam',
     u'',
 )
]

# A comma-separated list of custom stylesheets. Example:
pdf_stylesheets = ['letter','main','opensans']

# A list of folders to search for stylesheets. Example:
pdf_style_path = ['.', '_styles']

#pdf_compressed = False
pdf_font_path = ['fonts']
pdf_language = "en_US"
#pdf_fit_mode = "shrink"
pdf_break_level = 1
pdf_breakside = 'any'
#pdf_inline_footnotes = True
pdf_verbosity = 0
pdf_use_index = True
#pdf_use_modindex = True
pdf_use_coverpage = True
pdf_cover_template = 'custom.tmpl'
#pdf_appendices = []
#pdf_splittables = False
#pdf_default_dpi = 72
#pdf_extensions = ['vectorpdf']
#pdf_page_template = 'cutePage'
#pdf_use_toc = True
pdf_toc_depth = 3
pdf_use_numbered_links = True
pdf_fit_background_mode = 'scale'
