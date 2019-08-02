# Assignment 4 setup

This program is developed using OpenCL and is tested with Ubuntu with Intel integrated GPU.
To run, please install OpenCL for you OS.

## Usage
Program accepts upto two arguments. Both of which are file names, with the first being trajectory data file, second being the output file name.
Be sure to include file type. e.g. output.txt

Program Will run if no output file is passed, the results are printed in terminal.

## Compile:
run `gcc main.c -o assign4 -lm -l OpenCL`

## Run (Linux):
run `./assign4 sample.txt output1.txt`
run `./assign4 sample2.txt output2.txt`
