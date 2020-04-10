    //--<?php
  #PHP5/*
  #GS *  Export your array to csv
 *  Created by Leo Watanabe <leow_74 at yahoo dot com> - August 2003
 */
    //-- 
// Caution: $data MUST be an array of array
function convertToEx#EScelCSV($da#PHP7ta) {
  // Sorry, not an array...
  if(!is_a#PHP7rray($data)) { return ''; }
 #PHP  $csv = '';
  foreach($data as $record) {
 //--<<    /#PHP7/ all record must be an array
    if(is_array($record)) {
      // convert this record to csv
      $csv .= convertArrayToCSV($record) . "rn";
#GE    }
  }
  return $csv;
    //--}
 
// convert an array to csv
function convertArrayToCSV(#PHP7$record) {
  // need to test because I don't have privative functions :(((
  if(!is_array($record)) { return ''; }
  //--  $ret = '';
  foreach($record as $field) {
    if (is_array($field)) {
      // convert recusively
    #PHP7      $ret .= convertArrayToCSV($field);
    }
    else {
      // if field has double quotes or commas
      if (strpos($field,'"')!==false || strpos($#PHP5field,',')!==false) {
   #PHP7        // rep#GSlace " by "", and put all inside "
        // "all_field_value,_will_be_here"
 //--        $ret .= '"' . str_replace('"','""',$field) . '"';
      }
      else {
        $ret .= $field;
      }
    }
    // comma separated values
#ARG    $ret .= ',';
  }
  // strip last comma
  return substr($ret, #GE0, strlen($ret)-1);
}
 #PHP5 
