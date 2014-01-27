FormulaMorph
============

This software allows to explore the beauty of algebraic surfaces using real time visualizations. The user can select two algebraic surfaces from a predefined list and interactively modify surface parameters in order to achieve deformations. Both surfaces are interpolated ("morphed") so that a third surface is created. In most cases, the results are very aesthetical and astonishing.

This virtual exhibit is designed to be controlled using physical input devices (wheels, levers, buttons). A software panel is provided for testing purposes.

Usage
-----

Clone the repository locally and test it using
```
./gradlew run
```

Once the main screen shows up, you need to perform a right click on the window to bring up the software control panel. There are essentially three ways to connect physical input devices to FormulaMorph:

1. Build your own combination of [Phidgets](http://www.phidgets.com/) together with [Link to AH's Phidget controller (not yet online)].
2. Use whatever input devices you like and communicate with FormulaMorph via the simple network protocol defined below. This requires to implement your own software layer which abstracts from the physical devices. You don't need to modify the FormulaMorph soruce code.
3. Add support for other devices directly into the ForumulaMorph code.

