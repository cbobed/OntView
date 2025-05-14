# OntView – Ontology Viewer

Tool for the visualization and analysis of complex ontology structures.

![java](https://img.shields.io/badge/java–17-blue)
![maven](https://img.shields.io/badge/maven–3.9.6-blue)

We are currently studying the licenses of the APIs used to define the most appropriate one, but we are aiming at Apache 2.0 License, MIT, or GPL 3.0. 

---

## Key Features

This visualizer offers features such as observing the class and properties hierarchies, handling anonymous classes and GCIs, and summarizing the information showed to the user. Moreover, it allows to modify the layout of the different visual elements, and export both visual states and images, making it a powerful tool for both ontology creators and users.

- **Meaningful Semantic Visualization**  
  Render ontology models following the "what you see is what you meant" paradigm with the help of a Description Logics reasoner (any DL reasoner supporting OWLAPI 5.* can be easily plugged into the viewer). Moreover, anonymous classes and descriptions, as well as General Concept Inclusions (GCIs) are not left behind, and are included to actually give the required view to aprehend the actual semantics.

- **Interactive Navigation**  
  Explore classes, properties, and axioms hidding and expanding them in an interactive way. OntView keeps track of the actual reasoned model and provides means to tell the user about relationships that might be hidden. Besides, the user is continually provided with information about the size of the hidden/visible fragments of the ontology to have a broader view on the whole domain. Finally, OntView makes it possible to search to search for ontology terms in real time.

- **Information Overload Counter-measures**
  As the amount of information might be overwhelming for the user, OntView, apart from allowing hidding/showing different nodes in the graph, implements different features to reduce this potential overload:
  - **Detail Control**: In general, all the anonymous classes and GCIs are visualized. However, if the user is interested in just one particular part of the conceptual hierarchy, OntView provides means to focus the detailing procedure by describing a hierarchy fragment defined by two concepts that must have a subsumption relationship (by default, OntView details from TOP/OWL:Thing to BOTTOM/OWL:Nothing). This allows the user to focus on the particular subdomain that she is interested in. 
  - **Ontology Summarization**: Showing the whole ontology at once is nice, but it's not always useful. Thus, OntView exploits different relevance algorithms to assess the importance of the different concepts to select which nodes are to be initially shown. This mechanism is implemented as a plugin, and can be easily extended to give your own approach. In the current implementation, we provide three main summarizing methods based on:
    - KCE (Key Concept Extraction techniques as proposed by Peroni et al. [1]): measure that assess the importance of the different concepts according to a series of cognitive-, statistical- and topological-measures (However, they only consider named concepts). The original source code can be found in [Silvio's github repo](https://github.com/essepuntato/KCE).
    - PageRank/RDFRank: graph centrality measures which allow to consider as well the relevance of anonymous classes.
    - "Do your own summary": sometimes it's important just to see how a set of different concepts might be related within the ontology. OntView makes it possible so by allowing to select the concepts to be displayed (while, of course, showing the implicit relationships). 
  - **Controlled Expansion**: As the amount of children/parents of a concept in an ontology might be too many so as to be comprehend the structure, OntView allows the user to expand the nodes in a controlled way, specifying the amount of nodes that she wants to expand step by step (the selection of such nodes is done by relevance, but many different strategies can be implemented and plugged in without effort).   

- **Visual and Status Export**  
  OntView allows to capture and export your ontology views directly to **PNG** or **XML** formats, and, as the effort to make your ontology view might be high, store the current status of the graph to be able to reproduce it afterwards keeping track of the options and different layouts that the user have applied.

[1] Peroni, S., Motta, E., d’Aquin, M. (2008). Identifying Key Concepts in an Ontology, through the Integration of Cognitive Principles with Statistical and Topological Measures. In: Domingue, J., Anutariya, C. (eds) The Semantic Web. ASWC 2008. Lecture Notes in Computer Science, vol 5367. Springer, Berlin, Heidelberg. https://doi.org/10.1007/978-3-540-89704-0_17

---

## 🛠️ Getting Started 🛠️

Although it's completely independent of the underlying OS (the only potential dependency is to have the appropriate version of JavaFX installed), for convenience, we provide two scripts for Linux-like OS environments to install the dependencies and run OntView. 

### Install dependencies

Script to install the necessary JAR files before launching the application (the script downloads Maven if it is not available in the environment). 

```bash
sh ./install_dependencies.sh
```

### Run application

Script to clean, compile, and run the Java application using Maven.

```bash
sh ./execute.sh
```

---
