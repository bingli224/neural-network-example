
/**
 * @author BingLi224
 * @version	2019.02.03
 * 
 * ARFF-to-DataSet Conversion
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

class ArffParser {
	companion object {
		fun extractDataSet ( title : String, filename : String ) : DataSet {
			return FileInputStream ( filename ).use {
				extractDataSet ( title, it )
			}
		}

		fun extractDataSet ( title : String, inputStream : InputStream ) : DataSet {
			val br = BufferedReader ( InputStreamReader ( inputStream ) )
			
			var labelNames : Array <String> ? = null
			val features = ArrayList <DataSet.Feature> ( )
			//val sampleSet = mutableListOf <DoubleArray> ( )
			val sampleSet = mutableListOf <Array <Any>> ( )

			val REGEX_CSV_ROW_SPLIT = Regex ( """['"]?\s*,\s*['"]?""" )
			val REGEX_LEFT_BRACKET = Regex ( """^\s*\{\s*['"]?""" )
			//val REGEX_RIGHT_BRACKET = Regex ( """['"]?\s*\}\s*$""" )
			val REGEX_RIGHT_BRACKET = Regex ( """['"]?\s*\}[^}]*$""" )

			/**
			 * parsing mode
			 *	0 - find @attribute
			 *	1 - find @data
			 */
			var mode = 0

			var line : String ?
			val classList = ArrayList <Array <String> ?> ( )
			var originalAttributeCount = 0
			while ( true ) {
				line = br.readLine ( )
				if ( line == null )
					break

				//line ?. let {
					@Suppress ( "NAME_SHADOWING" )
					line = line.trim ( )
					when ( mode ) {
						0 -> {
							//val dat = line.split ( "['\"]?\\s+['\"]?".toRegex ( ), 3 )
							var dat = line.split ( "\\s+".toRegex ( ), 2 )
							when ( dat [ 0 ].toLowerCase ( ) ) {
								"@attribute" -> {
									// get the features
									if ( dat [ 1 ].startsWith ( "'" ) )
									{
										dat = dat [ 1 ]
											.replaceFirst ( "'", "" )
											.split ( Regex ( "'\\s+" ), 2 )
									}
									else if ( dat [ 1 ].startsWith ( "\"" ) )
									{
										dat = dat [ 1 ]
											.replaceFirst ( "\"", "" )
											.split ( Regex ( "\"\\s+" ), 2 )
									}
									else
									{
										dat = dat [ 1 ].split ( Regex ( "\\s+" ), 2 )
									}

									if ( dat [ 1 ].startsWith ( "{" ) ) {
										while ( classList.size < originalAttributeCount )
											classList.add ( null )

										classList.add ( dat [ 1 ]
											.replaceFirst ( REGEX_LEFT_BRACKET, "" )
											.replaceFirst ( REGEX_RIGHT_BRACKET, "" )
											.split ( REGEX_CSV_ROW_SPLIT )
											.toTypedArray ( )
											.also {
												it.forEach {
													features.add (
														DataSet.Feature (
															"${dat [ 0 ]} - $it",
															//dat [ 0 ]
															"class"
														)
													)
												}
												// assume that this is the last column, as the output
												labelNames = it
											}
										)
//println ( "========== ${labelNames?.joinToString ( "===" )}\tfeatures.size=${features.size}\tlastfeat=${features[features.size-1].name}\tcls=${classList [ classList.size - 1 ]?.joinToString ( "-" )}" )
//features.forEach { println ( "\t$it" ) }
									}
									else
									{
										// assume that this is the last column, as the output
										labelNames = arrayOf <String> ( dat [ 0 ] )

										features.add (
											DataSet.Feature (
												dat [ 0 ],	// name
												dat [ 1 ].toLowerCase ( )	// type
											)
										)
									}

									originalAttributeCount ++
								}
								"@data" -> {
									// next, get the source
									mode = 1

									// assume that the last feature is the label, so remove from the feature list
									//val labelType = features [ features.size - 1 ].type
									//features.retainAll { it.type != labelType }

									//for ( idx in 1 .. labelNames.size ) {
									//	features.removeAt ( features.lastIndex )
									//}
									labelNames ?. run {
										features.subList ( features.size - this.size, features.size )
											.clear ( )
									}
//println ( features.size )
//features.forEach { println ( "feat=$it" ) }
//labelNames?.forEach { println ( "label=$it" ) }
								}
								else -> { }	// unknown data
							}
						}
						else -> let {
							if ( ! line.startsWith ( "%" ) ) {
								// parse the sample
								val dat = line.split ( REGEX_CSV_ROW_SPLIT )
								var classChoiceLeft = -1
								var currentClassChoices : Array <String> ? = null
								var originalIdx = -1
//println ( "dat.size=${dat.size}\torgCount=$originalAttributeCount" )
								if ( dat.size == originalAttributeCount )
								{
//println ( "f.size=${features.size}" )
									// found the sample
									sampleSet.add ( Array <Any> ( features.size + 1, { newFeatureIdx ->
										if ( classChoiceLeft > 1 ) {
											// compare to the choices
											classChoiceLeft --
											currentClassChoices ?. let { currentClassChoices ->
										
/*
println ( "classChoiceLeft = $classChoiceLeft" )
println ( currentClassChoices.size )
currentClassChoices.forEach { println ( it ) }
println ( "-----${dat [ originalIdx ]}----------" )
// */

												if ( currentClassChoices [ currentClassChoices.size - classChoiceLeft ] == dat [ originalIdx ] )
													1.0	// this class
												else
													0.0	// not this class
											} ?: let {
												throw RuntimeException ( "Cannot find class choices while comparing to current parsed value." )
												//0	// impossible to not found the choices
											}
										} else {
											originalIdx ++
											val value = dat [ originalIdx ]
//println ( "newfeatidx=$newFeatureIdx originalIdx=$originalIdx" )
//if ( originalIdx < dat.lastIndex ) println ( "\tval=$value\tfeat[$newFeatureIdx].type=${features[newFeatureIdx].type}" )
//if ( originalIdx == dat.lastIndex ) println ( "\tval=$value\tlabels=${labelNames?.joinToString()}" )
											if ( newFeatureIdx >= features.size ) {
												value
											} else if ( features [ newFeatureIdx ].type == "int" ) {
												value.toLong ( )
											} else if ( features [ newFeatureIdx ].type == "real" || features [ newFeatureIdx ].type == "numeric" ) {
												value.toDouble ( )
											} else if ( features [ newFeatureIdx ].type == "class" ) {
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
											//else if ( features [ newFeatureIdx ].type == "class" || features [ newFeatureIdx ].type == "type" )
												//value.toDouble ( )
											} else {
												value	// string as default
											}
										}
									} ) )
								} // if: found sample
							} // if: line that's not comment
						} // else: find the sample
					} // when: parsing mode
				//} // let: found line data
			} // when: true
//println ( sampleSet [ 0 ].joinToString ( "-" ) )
			labelNames ?. run {
				return DataSet (
					title,
					this,
					features,
					sampleSet
				)
			}
			
			throw IllegalArgumentException ( "Cannot find data in given InputStream" )
		} // fun: create DataSet from given InputStream
	} // static
} // class: ARFF Parser
