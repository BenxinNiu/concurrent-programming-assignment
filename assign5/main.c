/*
Benxin Niu
*/
#include <stdio.h>
#include <mpi.h>
#include <time.h>
#include <stdlib.h>
#include <stdbool.h>

int * merge(int *A, int asize, int *B, int bsize);
void merge_sort(int *A, int min, int max);
void write_result(char filename[], int seg_id, int *segment);
int * sort_random_arr(int data_size, char filename[]);
int * generate_random_arr(int data_size);
bool predicate_arr_check(const int *sorted_arr, int size);

int main(int argc, char **argv)
{
    MPI_Init(&argc,&argv);

    // run 1
    double start = clock();
    int *sorted = sort_random_arr(4096, "result_4096");
    printf("\n\nArray with size of %d: Sorting finished, total time: %f seconds \n\n", 4096, (clock()-start)/CLOCKS_PER_SEC);
    if(!predicate_arr_check(sorted, 4096)) printf("Sorting failed......");

    // run 2
    start = clock();
    sorted = sort_random_arr(8192, "result_8192");
    printf("\n\nArray with size of %d: Sorting finished, total time: %f seconds \n\n", 8192, (clock()-start)/CLOCKS_PER_SEC);
    if(!predicate_arr_check(sorted, 8192)) printf("Sorting failed......");

    MPI_Finalize();

}

bool predicate_arr_check(const int *sorted_arr, int size){
    for(int i=0; i < size-1; i++){
        if(sorted_arr[i] > sorted_arr[i+1])
            return false;
    }
    return true;
}

int * generate_random_arr(int data_size){
    int *arr = (int *)malloc(data_size * sizeof(int));
    srandom(clock());
    for(int i=0; i < data_size; i++)
        arr[i] = (int)random();
    return arr;
}

void write_result(char filename[], int seg_id, int *segment){
    FILE *output = fopen(filename, "w");
    if (output !=NULL)
        for(int i=0; i < seg_id; i++){
            fprintf(output, "%d\n", segment[i]);
        }
    fclose(output);
}

void merge_sort(int *A, int min, int max)
{
    int mid = (min+max)/2;
    int lowerCount = mid - min + 1;
    int upperCount = max - mid;

    /* If the range consists of a single element, it's already sorted */
    if (max == min) {
        return;
    } else {
        /* Otherwise, sort the first half */
        merge_sort(A, min, mid);
        /* Now sort the second half */
        merge_sort(A, mid + 1, max);
        /* Now merge the two halves */
        merge(A + min, lowerCount, A + mid + 1, upperCount);
    }
}

int * sort_random_arr(int data_size, char filename[]){
    int *rnd_arr, *partition, *segment;
    int m = data_size;
    int processes, id;
    int seg_id = 0;

    // init
    MPI_Comm_rank(MPI_COMM_WORLD,&id);
    MPI_Comm_size(MPI_COMM_WORLD,&processes);
    MPI_Status m_status;

    if(id==0) {
        seg_id = data_size / processes;
        segment = (int *)malloc(seg_id * sizeof(int));
        rnd_arr=generate_random_arr(data_size);

        MPI_Bcast(&seg_id, 1, MPI_INT, 0, MPI_COMM_WORLD);
        MPI_Scatter(rnd_arr, seg_id, MPI_INT, segment, seg_id, MPI_INT, 0, MPI_COMM_WORLD);
        merge_sort(segment, 0, seg_id - 1);
    }
    else {
        printf("MPI_Init must be executed in main thread");
        exit(0);
    }
    int step = 1;
    while(step < processes) {
        if(id+step >= processes) {
            MPI_Send(&seg_id, 1, MPI_INT, id-step, 0, MPI_COMM_WORLD);
            MPI_Send(segment, seg_id, MPI_INT, id-step, 0, MPI_COMM_WORLD);
            break;
        }
        else {
            MPI_Recv(&m,1,MPI_INT,id+step,0,MPI_COMM_WORLD,&m_status);
            partition = (int *)malloc(m * sizeof(int));
            MPI_Recv(partition, m, MPI_INT, id + step, 0, MPI_COMM_WORLD, &m_status);
            segment = merge(segment, seg_id, partition, m);
            seg_id = seg_id + m;
        }
        step = step*2;
    }
    if(id==0) {
        write_result(filename, seg_id, segment);
    }
    return segment;
}

int * merge(int *A, int asize, int *B, int bsize) {
    int *C;
    int csize = asize+bsize;
    int ai = 0;
    int bi = 0;
    int ci = 0;

    C = (int *)malloc(csize*sizeof(int));
    while ((ai < asize) && (bi < bsize)) {
        if (A[ai] <= B[bi]) {
            C[ci] = A[ai];
            ci++; ai++;
        } else {
            C[ci] = B[bi];
            ci++; bi++;
        }
    }

    if (ai >= asize)
        for (int i = ci; i < csize; i++, bi++)
            C[i] = B[bi];
    else if (bi >= bsize)
        for (int i = ci; i < csize; i++, ai++)
            C[i] = A[ai];

    for (int i = 0; i < asize; i++)
        A[i] = C[i];
    for (int i = 0; i < bsize; i++)
        B[i] = C[asize+i];

    return C;
}
