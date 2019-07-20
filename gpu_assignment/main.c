#include <stdio.h>
#include <stdlib.h>

#ifdef __APPLE__
#include <OpenCL/opencl.h>
#else
#include <CL/cl.h>
#endif

#define MAX_SOURCE_SIZE (0x100000)

#include <math.h>
void calculate_distance(float *trajectory, float *distance, int i);


void calculate_distance(float *raw_data, float *distance, int i){

    int num_stop = (int) raw_data[1];
    int start = 2 + i*num_stop * 2;
    int end = start + num_stop * 2;
    float x = pow(raw_data[start] - 0, 2);
    float y = pow(raw_data[start +1] - 0, 2);
    float sum = sqrt(x+y);
    if (num_stop == 1){
        distance[i] = sum;
    }
    else {
        for (int k=start+2; k<end; k+=2){
            x = pow(raw_data[k] - raw_data[k-2], 2);
            y = pow(raw_data[k+1] - raw_data[k-1], 2);
            sum += sqrt(x+y);
        }
        distance[i] = sum;
    }
}

void sort_by_distance(float *raw_data, float *distance, float *output) {

}


int main() {
    int i;

    FILE *file = fopen("./sample.txt", "r"); /* should check the result */
    float *raw_data = (float *)malloc(sizeof(float)*MAX_SOURCE_SIZE);
    float n;
    int idx = 0;
    const int bound = MAX_SOURCE_SIZE/ sizeof(float);
    // prevent stack smashing
    while (fscanf(file, " %f", &n) == 1 && idx < bound) {
        raw_data[idx] = n;
        idx++;
    }
    const int num_trajectory = (int) raw_data[0];

//    float distance[4];
//    calculate_distance(raw_data, distance, 3);


    printf("Even I die, I'm the hero (in memory of Tony)!\n");

    // Load the kernel source code into the array source_str
    FILE *kernel_file;
    char *source_str;
    size_t source_size;

    kernel_file = fopen("distance_calculation.cl", "r");
    if (!kernel_file) {
        fprintf(stderr, "Unable to load kernel.\n");
        exit(1);
    }
    source_str = (char*)malloc(MAX_SOURCE_SIZE);
    source_size = fread( source_str, 1, MAX_SOURCE_SIZE, kernel_file);
    fclose( kernel_file );

    // Get platform and device information
    cl_platform_id platform_id = NULL;
    cl_device_id device_id = NULL;
    cl_uint ret_num_devices;
    cl_uint ret_num_platforms;
    cl_int ret = clGetPlatformIDs(1, &platform_id, &ret_num_platforms);
    ret = clGetDeviceIDs( platform_id, CL_DEVICE_TYPE_DEFAULT, 1,
                          &device_id, &ret_num_devices);

    // Create an OpenCL context
    cl_context context = clCreateContext( NULL, 1, &device_id, NULL, NULL, &ret);

    // Create a command queue
    cl_command_queue command_queue = clCreateCommandQueue(context, device_id, 0, &ret);


    // Create memory buffers on the device for each vector
    cl_mem raw_data_mem_obj = clCreateBuffer(context, CL_MEM_READ_ONLY,
                                      MAX_SOURCE_SIZE* sizeof(float), NULL, &ret);

    cl_mem distance_data_mem_obj = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                                                num_trajectory* sizeof(float), NULL, &ret);

    // Copy the raw data to their respective memory buffers
    ret = clEnqueueWriteBuffer(command_queue, raw_data_mem_obj, CL_TRUE, 0,
                               MAX_SOURCE_SIZE* sizeof(float), raw_data, 0, NULL, NULL);

    // Create a program from the kernel source
    cl_program program = clCreateProgramWithSource(context, 1,
                                                   (const char **)&source_str, (const size_t *)&source_size, &ret);

    // Build the program
    ret = clBuildProgram(program, 1, &device_id, NULL, NULL, NULL);

    // Create the OpenCL kernel
    cl_kernel kernel = clCreateKernel(program, "calculate_distance", &ret);

    // Set the arguments of the kernel
    ret = clSetKernelArg(kernel, 0, sizeof(cl_mem), (void *)&raw_data_mem_obj);
    ret = clSetKernelArg(kernel, 1, sizeof(cl_mem), (void *)&distance_data_mem_obj);

    // Execute the OpenCL kernel on the list
    size_t global_item_size = 1; // Process the entire lists
    size_t local_item_size = num_trajectory; // Divide work items into groups of 4
    ret = clEnqueueNDRangeKernel(command_queue, kernel, 1, NULL,
                                 &global_item_size, &local_item_size, 0, NULL, NULL);

    // Read the memory buffer distance on the device to the local variable distance
    float *distance = (float*)malloc(num_trajectory* sizeof(float));
    ret = clEnqueueReadBuffer(command_queue, distance_data_mem_obj, CL_TRUE, 0,
                              num_trajectory* sizeof(float), distance, 0, NULL, NULL);

    // Display the result to the screen
    for(i = 0; i < num_trajectory; i++)
        printf(" %f ", distance[i]);


    // Clean up
    ret = clFlush(command_queue);
    ret = clFinish(command_queue);
    ret = clReleaseKernel(kernel);
    ret = clReleaseProgram(program);
    ret = clReleaseMemObject(raw_data_mem_obj);
    ret = clReleaseMemObject(distance_data_mem_obj);
    ret = clReleaseCommandQueue(command_queue);
    ret = clReleaseContext(context);
    free(raw_data);
    free(distance);
    return 0;
}