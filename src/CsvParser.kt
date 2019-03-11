
/**
 * @author BingLi224
 * @version	2019.02.03
 * 
 * CSV-to-DataSet Conversion
 */

import java.util.Random
import java.util.LinkedList
import java.util.ArrayList
import java.io.InputStream
import java.io.InputStreamReader
import java.io.FileInputStream
import java.io.BufferedReader
import kotlin.math.pow
import kotlin.math.E

import DataSet

class CsvParser {
	companion object {
		fun extractDataSet ( title : String, filename : String, outputIndex : Int = -1 ) : DataSet {
			return FileInputStream ( filename ).use {
				extractDataSet ( title, it, outputIndex )
			}
		}

		fun extractDataSet ( title : String, inputStream : InputStream, outputIndex : Int = -1 ) : DataSet {
			val br = BufferedReader ( InputStreamReader ( inputStream ) )

			var line : String ?

			// get the attribute names

			line = br.readLine ( )
			if ( line == null )
				// cannot find the attribute names
				throw RuntimeException ( "Cannot find attribute list from given InputStream" )

			//val REGEX_CSV_ROW_SPLIT = Regex ( """['"]?\s*,\s*['"]?""" )
			val attributes = arrayOf <String> (
				*line.split ( "," ).toTypedArray ( )
			)
			// extract the data
			val dataTable = ArrayList <Array <Any>> ( )
			val classList = arrayOfNulls <ArrayList <String> ?> ( attributes.size )
			
			val features = ArrayList <DataSet.Feature> ( )
			val sampleSet = mutableListOf <Array <Any>> ( )

			val REGEX_NUMERIC = Regex ( """\d+(\.\d+)?""" )

			while ( true ) {
				line = br.readLine ( )
				if ( line == null )
					break

				//line ?. let {
					val rawDataRow = line.split ( "," )
					dataTable.add ( Array <Any> ( rawDataRow.size ) { idxData ->
						val rawData = rawDataRow [ idxData ]
						if ( rawData.matches ( REGEX_NUMERIC ) ) {
							// if numeric, remember as Double
							rawData.toDouble ( )
						} else {
							// if string, remember its choice
							classList [ idxData ] ?: run {
								// create the list of possible choices
								classList [ idxData ] = ArrayList <String> ( )
							}

							classList [ idxData ] ?. let {
								// remember this choice
								if ( ! it.contains ( rawData ) )
									it.add ( rawData )
							}
							
							// save original value
							rawData
						}
					} )
				//} // let: got line of data
			} // while: true

			// create new label names
			var labelNames = ArrayList <String> ( )

			// count new column size
			classList.forEachIndexed { idx, name ->
				if ( idx == outputIndex ) {
					// this column is the output
					name ?. run {
						name.forEach {
							//labelNames.add ( "${attributes [ idx ]} - ${it}" )
							labelNames.add ( it )
						}

						Unit
					} ?: run {
						labelNames.add ( attributes [ idx ] )
					}
				} else {
					// this column is a feature
					name ?. run {
						name.forEach {
							features.add (
								DataSet.Feature (
									"${attributes [ idx ]} - ${it}",
									"numeric"
								)
							)
						}

						Unit
					} ?: run {
						features.add (
							DataSet.Feature (
								attributes [ idx ],
								"string"
							)
						)
					}
				}
			}
//println ( "label.size=${labelNames.size} features.size=${features.size}" )
			// convert the string data to boolean choices
			dataTable.forEach { cell ->
				var classChoiceLeft = -1
				var currentClassChoices : ArrayList <String> ? = null
				var originalIdx = -1
				sampleSet.add ( Array <Any> ( features.size ) {

					if ( classChoiceLeft > 1 ) {
						// compare to the choices
						classChoiceLeft --
						currentClassChoices ?. let { currentClassChoices ->
/*
println ( "classChoiceLeft = $classChoiceLeft" )
println ( "currentClassChoices.size=${currentClassChoices.size}" )
currentClassChoices.forEach { println ( it ) }
println ( "cell.size=${cell.size}" )
println ( "-----${cell [ originalIdx ]}----------" )
// */

							if ( currentClassChoices [ currentClassChoices.size - classChoiceLeft ] == cell [ originalIdx ] )
								1.0	// this class
							else
								0.0	// not this class
						} ?: let {
							throw RuntimeException ( "Cannot find class choices while comparing to current parsed value." )
							//0	// impossible to not found the choices
						}
					} else {
						originalIdx ++
//println ( "oriIdx=$originalIdx outputIdx=$outputIndex features.size=${features.size}" )
						if ( originalIdx == outputIndex )
							originalIdx ++	// skip the label

						if ( originalIdx == cell.size )
							cell [ outputIndex ]
						else {
							val value = cell [ originalIdx ]
							when ( value ) {
								is Double -> value
								else -> {
									classList [ originalIdx ] ?. let { choices ->
										// remember the choices of current class
										currentClassChoices = choices

										// remember the current index of choice
										classChoiceLeft = choices.size

										// check with the choice
										if ( choices [ 0 ] == value )
											1.0	// this class
										else
											0.0	// not this class
									} ?: let {
										throw RuntimeException ( "Cannot find class choices while comparing to current parsed value." )
										//""	// unknown data
									}
								}
							} // when: type of feature
						} // if-else: feature, not the output
					} // if-else: features
				} )
			} // foreach: data row

			/*
			labelNames.forEach {
				println ( "label:\t$it" )
			}
			features.forEach {
				println ( "feature:\t${it.name}" )
			}
			println ( "${sampleSet [ 0 ].joinToString ( ", ", "{", "}" )}" )
			sampleSet.forEach {
				println ( "${it.joinToString ( ", ", "{", "}" )}" )
			}
			*/

			return DataSet (
				title,
				labelNames.toTypedArray ( ),
				features,
				sampleSet
			)
		} // fun: create DataSet from given InputStream
	} // static
} // class: CSV parser
