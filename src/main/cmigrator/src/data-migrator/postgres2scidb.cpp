#include "postgres2scidb.h"
#include "postgres.h"
#include "buffer.h"

#define BUFFER_SIZE 65536
//#define BUFFER_SIZE 104857600

int Postgres2Scidb::postgres2scidb(FILE* inFile, std::vector<boost::shared_ptr<Attribute> > &attributes, FILE* outFile) {
    //Postgres::skipHeader(inFile);
    Postgres::readHeader(inFile);
    Buffer buffer;
    BufferNew(&buffer,inFile,BUFFER_SIZE);
    // in each step of the loop we process one line
    //while (Postgres::readColNumberBuffer(&buffer) != -1) {
    while (Postgres::readColNumber(inFile) != -1) {
        // process each column in a line
        for (std::vector<boost::shared_ptr<Attribute> >::iterator it=attributes.begin(); it != attributes.end(); ++it) {
            //(*it)->postgresReadBinaryBuffer(&buffer);
	    (*it)->postgresReadBinary(inFile);
            (*it)->scidbWriteBinary(outFile);
        }
    }
    BufferDispose(&buffer);
    fclose(inFile);
    fclose(outFile);
    return 0;
}
