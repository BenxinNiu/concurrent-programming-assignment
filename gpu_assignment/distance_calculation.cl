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

__kernel void sort_by_distance(__global int *reference_idx, __global float *distance, __global int *n) {
    // sorted_idx[0] = distance[2];
     int i, j;
        for (i = 0; i < *n-1; i++)
            for (j = 0; j < *n-i-1; j++)
                if (distance[j] > distance[j+1]){
                    float tmp = distance[j];
                    distance[j] = distance[j+1];
                    distance[j+1] = tmp;
                    int tmpIdx = reference_idx[j];
                    reference_idx[j] = reference_idx[j+1];
                    reference_idx[j+1] = tmpIdx;
                }
}
