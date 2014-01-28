FormulaMorph
============

This software allows to explore the beauty of algebraic surfaces using real time visualizations. The user can select two algebraic surfaces from a predefined list and interactively modify surface parameters in order to achieve deformations. Both surfaces are interpolated ("morphed") so that a third surface is created. In most cases, the results are very aesthetical and astonishing!

![FormulaMorph screen shot](https://raw.github.com/IMAGINARY/FormulaMorph/gh-pages/images/FormulaMorphScreenShot.png)

This virtual exhibit is designed to be controlled using physical input devices (wheels, levers, buttons). A software panel is provided for testing purposes.

Usage
-----

Clone the repository locally and test it using
```
./gradlew run
```

Once the main screen shows up, you need to perform a right click on the window to bring up the software control panel. There are essentially three ways to connect physical input devices to FormulaMorph:

1. Build your own combination of [Phidgets](http://www.phidgets.com/) together with [Link to AH's Phidget controller (not yet online)]. This has been used at the MoMath NYC:
   ![FormulaMorph exhibit at the MoMath](https://raw.github.com/IMAGINARY/FormulaMorph/gh-pages/images/FormulaMorphAtMoMath.jpg)
2. Use whatever input devices you like and communicate with FormulaMorph via the simple [network protocol](#network-protocol) defined below. This requires to implement your own software layer which abstracts from the physical devices. You don't need to modify the FormulaMorph soruce code.
3. Add support for other devices directly into the ForumulaMorph code.

Network protocol
----------------

FormulaMorph can be controlled via the network. It acts as a client that connects to the server and port given in the file [settings.properties](settings.properties). The protocol itself is best explained using an example session.

```
# a comment
# S: message sent by the server
# C: message sent by the client (FormulaMorph)

C: # send a heart beat comment every second to recognize broken connections
S: # send a heart beat comment every second to recognize broken connections

S: FS,1,3     # select the formula from the list on the left which is 3 formulas ahead of the current one 
C: LD,1,1     # let server know that the new (left) fomula contains a parameter 'a',
              # (allows server to provide feedback to the user, e.g. by enabling the knob controlling this parameter)
C: LD,2,0     # new (left) formula does not contain parameter 'b' 
C: LD,3,0     # new (left) formula does not contain parameter 'c' 
C: LD,4,0     # new (left) formula does not contain parameter 'd' 
C: LD,5,0     # new (left) formula does not contain parameter 'e' 
C: LD,6,0     # new (left) formula does not contain parameter 'f'

C: # heart beat comment
S: # heart beat comment

S: RE,1,10    # increase the value of left parameter 'a' by 10/360 of its range
S: JW,1,5     # rotate the surfaces by moving 5 steps along the rotation path

S: FS,2,-1    # select the formula from the list on the right which is 3 formulas before the current one 
C: LD,7,1     # new (right) formula contains parameter 'a' 
C: LD,8,0     # new (right) formula does not contain parameter 'b' 
C: LD,9,1     # new (right) formula contains parameter 'c' 
C: LD,10,0    # new (right) formula does not contain parameter 'd' 
C: LD,11,0    # new (right) formula does not contain parameter 'e' 
C: LD,12,0    # new (right) formula does not contain parameter 'f'

C: # heart beat comment
S: # heart beat comment

S: RE,9,20    # increase the value of right parameter 'c' by 20/360 of its range

S: JS,1,0.125 # set the morph parameter to 0.125

S: SW,1,1     # save screenshot for the left surface
S: SW,1,2     # save screenshot for the right surface (currently the same as for the left surface)

C: # heart beat comment
S: # heart beat comment

...
```

Contribute & Collaborate
------------------------

FormulaMorph is part of [IMAGINARY](http://www.imaginary.org) by the [Mathematisches Forschungsinstitut Oberwolfach](http://www.mfo.de). It was originally developed for and in collaboration with the [National Museum of Mathematics, NYC](http://www.momath.org).

The design of the program was done in collaboration with [Moey Inc](http://moeyinc.com/), the company who also produced the hardware and hardware-software connection of the first exhibit.

If you are interested in showing FormulaMorph at your museum or exhibition, you may [contact us and ask for support](http://http://www.imaginary.org/contact). 
