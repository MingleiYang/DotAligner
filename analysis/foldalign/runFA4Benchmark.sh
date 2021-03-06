BIN=/home/stesee/bin
HDOTALIGNER=/home/stesee/DotAligner
PROG=foldalign
EXE=foldalign

DATA=$1
#DATA=seed_10_55.new
#DATA=seed_56_95.new

rm -f ${DATA}.${PROG};
for FAMSEQ1 in `ls ${HDOTALIGNER}/analysis/rfam/selected_PIDs/${DATA}/*.fa`;
do
  for FAMSEQ2 in `ls ${HDOTALIGNER}/analysis/rfam/selected_PIDs/${DATA}/*.fa`;
  do
    echo $FAMSEQ1 > tmp.famseq
    echo $FAMSEQ2 >> tmp.famseq
    if [[ `sort tmp.famseq | head -1` == $FAMSEQ1 ]];
    then

      echo "echo $FAMSEQ1 $FAMSEQ2 >> ${DATA}.${PROG};"
      echo "{ time ${EXE} -max_diff 25 -global -summary $FAMSEQ1 $FAMSEQ2; } &>> ${DATA}.${PROG};"

    fi;
  done;
done;

