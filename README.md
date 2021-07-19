# Flappy-Bird-NeuroEvolution
 Solving Flappy Bird with Neuroevolution
![Project View](https://i.imgur.com/eZiq09v.png)

## Background Information
This is my first successful Neuroevolution project! I've attempted to use Neuroevolution to solve the game of Snake last summer, but after months of coding it didn't end up working. This summer, with the help of Professor Daniel Shiffman's videos, I was able to learn things that I didn't do in the past (such as normalizing inputs), and this project ended up being a success!

## Dependencies/Libraries
This project uses a modified version of the simple Neural Network implementation by theKayani, [here is the link to his Github repo](https://github.com/theKayani/Java-Neural-Network).

This project also uses JFoenix for some UI components to make them more aesthetically pleasing, [you can find more information on JFoenix here](http://www.jfoenix.com/).

**You don't need to actually download any of these dependencies yourself, they're included in the project**

## How it Works
Neuroevolution is essentially a combination of Neural Networks and Genetic Algorithms. Normally, Neural Networks use Backpropagation to learn, which usually requires a large amount of data that is prepared beforehand in order to train the network. In Neuroevolution, the network kind of trains as it goes, and data is generated through the experiences of every bird.

If you've looked at my other projects that use Genetic Algorithms, you will notice that the genes I use for the entities/lifeforms are usually just integers or vectors. In Neuroevolution, the genes are the Neural Network's weights and biases. That's really the main difference here.

Note that there is some randomness at play here. Sometimes a good bird will appear on the first generation, sometimes the 100th generation. I have an option to run the simulation at 30x speed, so unless you're somehow extremely unlucky, training time should usually take between 20 seconds to 3 minutes.

## How to Train the Neural Networks
There are a lot of settings located in the bottom of the project, so you can really fine tune the neural networks as much as you want.

My approach to training to Neural Networks is to run it at 30x speed till 1 or more birds start to learn the game. After that, I slowly lower the gap size of the pipes (thus increasing the difficulty of the game), then run the program for a few more generations (rinse and repeat). The goal for me is to be able to train birds that can go through the smallest gap possible. In the main folder, there is a sample best bird data file where I've personally trained birds to be able to go through pipes with a gap size of 70 pixels almost perfectly using the method I described above.

Apparently the method I use has a formal term called Curriculum Learning. Pretty cool!

## Running the Project
Simply run the jar file that is located in the dist folder. If you want to move the jar file somewhere else, make sure to also move the lib folder with it as well, since it needs the jar file within the lib in order to load some of the UI components.

## Training the Best Bird Possible
If you've somehow able to train a bird that can solve the game with the pipe gap size of under 65 pixels, please email me/let me know how you did it! I know that it is possible for birds to pass through pipes with a gap size of 63, but I'm unable to get them to do so consistently.
