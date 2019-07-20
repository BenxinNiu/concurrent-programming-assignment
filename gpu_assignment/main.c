#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef __APPLE__
#include <OpenCL/opencl.h>
#else
#include <CL/cl.h>
#endif

#define MAX_SOURCE_SIZE (0x100000)

#define LSIZ 128

int main(int argc, char *argv[]) {
    char *source = argv[1];
    char *outputName = argv[2];
    if(source==NULL || outputName==NULL){
        printf("Not enough arugments provided \n \n "
               "Usage: first argument is the source file name, second is the output filename \n \n ");
    }
    else {
        printf("Source file specified: %s \n", source);
        printf("Output file specified: %s \n", outputName);
    }
    //Allow program to run without specifying output file...
    //result will be printed in terminal if no output file specified
    printf("Begin \n \n");
    int i;
    FILE *file = fopen(source, "r");
    if (file == NULL){
        printf("Unable to open source file: %s... exiting \n", source);
        exit(1);
    }
    float *raw_data = (float *)malloc(sizeof(float)*MAX_SOURCE_SIZE);
    float n;
    int idx = 0;
    const int bound = MAX_SOURCE_SIZE/ sizeof(float);
    // prevent stack smashing
    while (fscanf(file, " %f", &n) == 1 && idx < bound) {
        raw_data[idx] = n;
        idx++;
    }
    fclose(file);
    // read each line as char
    char line[LSIZ][LSIZ];
    FILE *fptr = fopen(source, "r");
    idx = 0;
    if(fptr == NULL){
        printf("Unable to open source file: %s... exiting \n", source);
        exit(1);
    }
    while(fgets(line[idx], LSIZ, fptr))
    {
        line[idx][strlen(line[idx]) - 1] = '\0';
        idx++;
    }
    fclose(fptr);
    const int num_trajectory = (int) raw_data[0];
    //create an reference index for each trajectory for later sorting;
    int *reference_idx = (int *)malloc(sizeof(int)*num_trajectory);
    for (int k=0; k<num_trajectory; k++){
        reference_idx[k] = k;
    }

    printf("\nEven I die, I'm the hero EIDITH (in memory of Tony)!\n \n");

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

    cl_mem distance_data_mem_obj = clCreateBuffer(context, CL_MEM_READ_WRITE,
                                                num_trajectory* sizeof(float), NULL, &ret);

    cl_mem reference_idx_mem_obj = clCreateBuffer(context, CL_MEM_READ_WRITE,
                                                  num_trajectory* sizeof(int), NULL, &ret);

    cl_mem num_trajectory_mem_obj = clCreateBuffer(context, CL_MEM_READ_ONLY, sizeof(int), NULL, &ret);


    // Copy the raw data to their respective memory buffers
    ret = clEnqueueWriteBuffer(command_queue, raw_data_mem_obj, CL_TRUE, 0,
                               MAX_SOURCE_SIZE* sizeof(float), raw_data, 0, NULL, NULL);
    // Copy reference index for sorting
    ret = clEnqueueWriteBuffer(command_queue, reference_idx_mem_obj, CL_TRUE, 0,
                               num_trajectory*sizeof(int), reference_idx, 0, NULL, NULL);
    // Copy reference index for sorting
    ret = clEnqueueWriteBuffer(command_queue, num_trajectory_mem_obj, CL_TRUE, 0,
                               sizeof(int), &num_trajectory, 0, NULL, NULL);

    // Create a program from the kernel source
    cl_program program = clCreateProgramWithSource(context, 1,
                                                   (const char **)&source_str, (const size_t *)&source_size, &ret);

    // Build the program
    ret = clBuildProgram(program, 1, &device_id, NULL, NULL, NULL);

    // Create the OpenCL kernel
    cl_kernel kernel = clCreateKernel(program, "calculate_distance", &ret);
    cl_kernel kernel2 = clCreateKernel(program, "sort_by_distance", &ret);

    // Set the arguments of the kernel
    ret = clSetKernelArg(kernel, 0, sizeof(cl_mem), (void *)&raw_data_mem_obj);
    ret = clSetKernelArg(kernel, 1, sizeof(cl_mem), (void *)&distance_data_mem_obj);


    ret = clSetKernelArg(kernel2, 0, sizeof(cl_mem), (void *)&reference_idx_mem_obj);
    ret = clSetKernelArg(kernel2, 1, sizeof(cl_mem), (void *)&distance_data_mem_obj);
    ret = clSetKernelArg(kernel2, 2, sizeof(cl_mem), (void *)&num_trajectory_mem_obj);

    // Execute the OpenCL kernel on the raw data (coordinates)
    size_t global_item_size = 1;
    size_t local_item_size = num_trajectory; // Divide work items into groups of # of trajectory
    ret = clEnqueueNDRangeKernel(command_queue, kernel, 1, NULL,
                                 &global_item_size, &local_item_size, 0, NULL, NULL);

    // Read the memory buffer distance on the device to the local variable distance
    float *distance = (float*)malloc(num_trajectory* sizeof(float));
    ret = clEnqueueReadBuffer(command_queue, distance_data_mem_obj, CL_TRUE, 0,
                              num_trajectory* sizeof(float), distance, 0, NULL, NULL);

    // Display the distance result to the screen
    printf("distance computation complete: \n");
    for(i = 0; i < num_trajectory; i++)
        printf(" %f ", distance[i]);

    printf("\nBegin sorting... \n");

    //PERFORM SORT in GPU
    global_item_size = 1;
    local_item_size = 1;
    ret = clEnqueueNDRangeKernel(command_queue, kernel2, 1, NULL,
                                 &global_item_size, &local_item_size, 0, NULL, NULL);

    // Read the memory buffer distance on the device to the local variable distance
    int *sorted = (int*)malloc(num_trajectory* sizeof(int));
    ret = clEnqueueReadBuffer(command_queue, reference_idx_mem_obj, CL_TRUE, 0,
                              num_trajectory* sizeof(int), sorted, 0, NULL, NULL);

    printf(" \nSorting complete, writing to file (), printed result as follows: \n");

    FILE* output = fopen(outputName, "w");
    if(output == NULL)
    {
        printf("Error outputting result to %s! \n", outputName);
        exit(1);
    }
    // Display the result to the screen
    for(i = 0; i < num_trajectory; i++){
        printf("Trajectory: %s  Distance: %f \n", line[sorted[i]+1], distance[sorted[i]]);
        fprintf(output,"Trajectory: %s  Distance: %f \n",line[sorted[i]+1], distance[sorted[i]]);
    }
    printf("\nResult are written to file: %s \n", outputName);
    fclose(output);

    // Clean up
    ret = clFlush(command_queue);
    ret = clFinish(command_queue);
    ret = clReleaseKernel(kernel);
    ret = clReleaseKernel(kernel2);
    ret = clReleaseProgram(program);
    ret = clReleaseMemObject(raw_data_mem_obj);
    ret = clReleaseMemObject(reference_idx_mem_obj);
    ret = clReleaseMemObject(num_trajectory_mem_obj);
    ret = clReleaseMemObject(distance_data_mem_obj);
    ret = clReleaseCommandQueue(command_queue);
    ret = clReleaseContext(context);
    free(raw_data);
    free(sorted);
    free(distance);
    return 0;
}