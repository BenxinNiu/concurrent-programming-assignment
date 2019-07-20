__kernel void calculate_distance(__global float *raw_data, __global float *distance){
// Since each trajectory are independent, distance are calculated in parallel
    int i = get_global_id(0);

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
