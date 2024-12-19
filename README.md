# FormulaMorph

This software allows to explore the beauty of algebraic surfaces using real time visualizations. The user can select two algebraic surfaces from a predefined list and interactively modify surface parameters in order to achieve deformations. Both surfaces are interpolated ("morphed") so that a third surface is created. In most cases, the results are very aesthetical and astonishing!

![FormulaMorph screen shot](https://raw.github.com/IMAGINARY/FormulaMorph/gh-pages/images/FormulaMorphScreenShot.png)

This virtual exhibit is designed to be controlled using physical input devices (wheels, levers, buttons). A software panel is provided for testing purposes.

## Usage

A precompiled version of FormulaMorph is available for download on the releases page. The software is written in Java and should run on all major operating systems. A local installation of a compatible Java Runtime Environment (JRE) is required. Tests have been performed successfully on Linux (Ubuntu 22.04) with OpenJDK 8 and OpenJDK 21.

To run FormulaMorph:

1. Download ZIP or TAR from the releases.
2. Extract the archive.
3. Change into the extracted folder (`FormulaMorph-VERSION`) or set the working directory to it.
4. Execute `./bin/FormulaMorph` (Linux/Mac) or `bin/FormulaMorph.bat` (Windows)

Once the main screen shows up, you need to perform a right click on the window to bring up the software control panel. There are essentially three ways to connect physical input devices to FormulaMorph:

1. Build your own combination of [Phidgets](http://www.phidgets.com/) together with [this Phidget controller](https://github.com/ahrv/FormulaMorph). This has been used at the MoMath NYC:
   ![FormulaMorph exhibit at the MoMath](https://raw.github.com/IMAGINARY/FormulaMorph/gh-pages/images/FormulaMorphAtMoMath.jpg)
2. Use whatever input devices you like and communicate with FormulaMorph via the simple [network protocol](#network-protocol) defined below. This requires to implement your own software layer which abstracts from the physical devices. You don't need to modify the FormulaMorph source code.
3. Add support for other devices directly into the ForumulaMorph code.

### Settings

Some settings can be adjusted in the file `settings.properties`. The following (default) settings are available:

```properties
# host and port for the server connected to the input devices (Phidgets)
phidget_host=localhost
phidget_port=4767

# start in fullscreen
enable_fullscreen=false

# allow easter egg sequence
enable_easter_egg=true

# gallery item that are not selected are shown with this color saturation
gallery_item_saturation=0.0

# screensaver robot starts after this number of seconds
screensaver_after_seconds=600

# rotation is done on the path defined in 'quaternions.txt';
# this defines the number of steps to walk along the whole path;
# since values are interpolated this is not related to the number of entries in 'quaternions.txt'
```

## Development

A compatible Java Development Kit (JDK) is required to compile the source code. OpenJDK 8 is known to work. The project uses the Gradle build system.

Clone the repository locally and test it using

```bash
./gradlew run
```

This will install all required dependencies, compile the code and start the application.

To recompile the code, execute

```bash
./gradlew compileJava
```

To build the whole project, including the files for redistribution, execute

```bash
./gradlew build
```

The distribution files are located in the `build/distributions` folder.

## Network protocol

FormulaMorph can be controlled via the network. It acts as a client that connects to the server and port given in the file [settings.properties](settings.properties). The protocol itself is best explained using an example session.

```
# a comment
# S: message sent by the server
# C: message sent by the client (FormulaMorph)
# The 'S: ' and 'C: ' prefixes are not part of the protocol and are only used here for clarity.

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

In order to test the network protocol, you can just create a new server using NetCat. On Mac and Linux systems it looks like this:

```bash
nc -l port
```

`port` has to be replaced by the port value defined in `settings.properties`. As soon as FormulaMorph connects to the server you see various messages comming in. Just type in a few of the commands listed above and see what happens (the server commands are prefixed with `S: ` in the above listing).

## Contribute & Collaborate

FormulaMorph is part of [IMAGINARY](http://www.imaginary.org) by the [Mathematisches Forschungsinstitut Oberwolfach](http://www.mfo.de). It was originally developed for and in collaboration with the [National Museum of Mathematics, NYC](http://www.momath.org).

The design of the program was done in collaboration with [Moey Inc](http://moeyinc.com/), the company who also produced the hardware and hardware-software connection of the first exhibit.

If you are interested in showing FormulaMorph at your museum or exhibition, you may [contact us and ask for support](https://www.imaginary.org/contact).
