Component name
==============

Short description
-----------------
The Aniketos ConSpec Editor is an Eclipse plugin that integrates with the Eclipse workbench and allows ConSpec XML files to be loaded in, edited, saved out and managed within other Eclipse projects. The editor provides a user interface for making changes to the ConSpec file.

Overview 
--------
ConSpec files are important for Aniketos since they are the main format used for specifying security requirements. Aniketos agreement templates, user security policies, security contracts and security descriptors are all presented in the form of ConSpec files.

The Aniketos ConSpec Editor is an Eclipse plugin that integrates with the Eclipse workbench and allows ConSpec XML files to be loaded in, edited, saved out and managed within other Eclipse projects. The editor provides a user interface for making changes to the ConSpec file.

The ConSpec new file wizard provides an Eclipse wizard that can be used to create a new minimal ConSpec file.

In case you just want to get up and running creating and editing ConSpec files as quickly as possible, this section presents a quick start guide.

Requirements
------------
ConSpecEdit integrates with the Eclipse IDE, which is available for Windows, Linux, OS X and other operating systems that support Java.

Features
--------
ConSpecEdit comes with the following features:
* Create security policy documents in the ConSpec XML format.
* Load, view and edit existing ConSpec XML documents.
* View ConSpec files in human-readable form and save out in XML.
* Specify all aspects of the ConSpec file through the user-interface.
* Provides an Eclipse Wizard for creating new files within Eclipse projects.
* Integrates the editor fully into Eclipse, so that ConSpec files can be automatically sent to the editor.

How to get started
------------------
Setting up and using ConSpecEdit is straightforward. The binary version can be downloaded and installed using the Eclipse Available Software browser. Just add the LJMU-Aniketos software repository:
[http://www.cms.livjm.ac.uk/nistl/dnload/aniketos/eclipse/releases/juno/](http://www.cms.livjm.ac.uk/nistl/dnload/aniketos/eclipse/releases/juno/)
From here you'll be able to click and install the software to be integrated into Eclipse.
Once installed, to create a new ConSpec file, simple choose the **ConSpec file** option under the **Aniketos** folder in the New file dialogue. Alternatively, click on an existing ConSpec file (with **.conspec** extension) in an Eclipse project to open the editor.
For more information, see the Installation instructions below, or read through the detailed project documentation.

Contributing (guide)
--------------------
ConSpecEdit is released under am LGPL licence. Please contact the project maintainers if you have contributions or would like to get involved. We'd be interested to hear from you if you'd like to contribute code, documentation, example files or anything else that you think might be useful for the project.

Installation
------------
1. Installation from the remote repository requires a recent version (1.5.1 or later) of [Eclipse](http://eclipse.org/).
2. To install the software, open the **Install New** Software dialogue from the **Help** menu. Add the following remote software repository:
[http://www.cms.livjm.ac.uk/nistl/dnload/aniketos/eclipse/releases/juno/](http://www.cms.livjm.ac.uk/nistl/dnload/aniketos/eclipse/releases/juno/)
3. Once added, **ConSpecEdit** will become available as an installable plugin under the **Aniketos** category. Install the software by selecting it and clicking through the dialogue boxes until the installation process is complete.
4. You can now create and edit ConSpec files using Eclipse.

Modules, APIs
-------------
ConSpecEdit is a standalone editor that integrates with the Eclipse UI. It doesn't have any particular APIs that are externally accessible. Data exchange is performed using the ConSpec XML format.

Usage manual
------------
For the complete usage manual, please find the documentation in the repository.

Example usage
-------------
For the complete usage manual, please find the documentation in the repository.

Credits
-------
ConSpecEdit was produced by members of the School of Computing and Mathematical Sciences at Liverpool John Moores University. It was produced as part of the [Aniketos Project](http://aniketos.eu/), partly funded by the European Community's Seventh Framework Programme under grant agreement 257930.

The current code was developed by [David Llewellyn-Jones](http://www.flypig.co.uk).

Official site, external resources
---------------------------------
1. More details about ConSpec can be found at the [ConSpecEdit site](http://www.flypig.co.uk?at=conspecedit).
2. For a more detailed explanation about the theory of ConSpec files, see [Aktug and Naliuka](http://people.cs.kuleuven.be/~pieter.philippaerts/inliner/papers/conspec.pdf).
3. To understand how ConSpec files are used and extended within Aniketos, refer to [D2.3: Models and methodologies for implementing Security-by-Contract for services](http://www.aniketos.eu/sites/default/files/deliverables/Aniketos%20D2.3%20Models%20and%20methodologies%20for%20implementing%20Security-by-Contract%20for%20services.pdf).

About the developers of this component
--------------------------------------
See the credits section above for more information about the developers of the component.

Updates and list of known issues
--------------------------------
The current version doesn't use the latest ConSpec XML Scheme, and can therefore not load all valid ConSpec files. The new schema will be used in the next release of the tool.

