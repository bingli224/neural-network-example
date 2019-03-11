
/**
 * @author BingLi224
 * @version	2019.02.03
 * 
 * Nueral Network : Classification
 *
 * Reference: http://galaxy.agh.edu.pl/~vlsi/AI/backp_t_en/backprop.html
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

typealias Neuron = Double
typealias NeuronArray = DoubleArray
typealias Weight = Double
typealias WeightArray = DoubleArray
typealias Bias = Double
typealias BiasArray = DoubleArray
typealias Label = Double
typealias LabelArray = DoubleArray
//typealias Array <Label> = DoubleArray

class Classifier (
	val dataSet : DataSet,
	val nHiddenNeurons : IntArray,	// number of neurons in each hidden layer
	var learningRate : Double = .05,
	var nEpoches : Long = 10L
) : Runnable {

	//val nHiddenNeurons : IntArray

	// neuron in hidden and output layers
	var outputLayers : Array <NeuronArray>
	//var outputLayers : ArrayList <NeuronArray>

	// weight between input/hidden/output
	var weightLayers : Array <WeightArray>
	//var weightLayers : ArrayList <WeightArray>

	// bias of each layers
	var biases : BiasArray

	// list of errors
	var errorSequence = LinkedList <Double> ( )

	// :TEMP: inputs = Doubles
	//var input : Array <Double>

	// array of label name
	//var labelList : Array <String>

	// list of current output
	var predictedOutput : LabelArray
	
	init {
		//this.nHiddenNeurons = nHiddenNeurons
		// initialize the output
		predictedOutput = LabelArray ( dataSet.labelNames.size )

		// find possible data in each features
		//val columeSize = dataSet.features.size

		val rnd = Random ( )

		// prepare the neurons and weights
		biases = BiasArray ( nHiddenNeurons.size ) { 0.00 }
		biases [ biases.lastIndex ] = 1.0	// try only bias in last lyaer

		//outputLayers = arrayOfNulls <NeuronArray ?> ( nHiddenNeurons.size + 1 )
		//weightLayers = arrayOfNulls <WeightArray ?> ( nHiddenNeurons.size + 1 )
		//outputLayers = ArrayList <NeuronArray> ( )
		//weightLayers = ArrayList <WeightArray> ( )
		outputLayers = Array <NeuronArray> ( nHiddenNeurons.size + 1 ) {
			if ( it < nHiddenNeurons.size )
				NeuronArray ( nHiddenNeurons [ it ] ) { 0.0 } //rnd.nextDouble ( ) }
			else
				NeuronArray ( dataSet.labelNames.size ) { 0.0 } //rnd.nextDouble ( ) }
		}
		weightLayers = Array <WeightArray> ( nHiddenNeurons.size + 1 ) {
			if ( it < 1 )
				WeightArray ( nHiddenNeurons [ it ] * dataSet.features.size ) { rnd.nextDouble ( ) }
			else if ( it < nHiddenNeurons.size )
				WeightArray ( nHiddenNeurons [ it ] * nHiddenNeurons [ it - 1 ] ) { rnd.nextDouble ( ) }
			else
				WeightArray ( nHiddenNeurons [ it - 1 ] * dataSet.labelNames.size ) { rnd.nextDouble ( ) }
		}
/*
println ( "!!!!!!${nHiddenNeurons.joinToString()}" )
println ( "biases [${biases.size}]============\n${biases.joinToString()}" )
println ( "outputs [${outputLayers.size}]============\n" )
outputLayers.forEach { println ( "${it.size}::[${it.joinToString()}]" ) }
println ( "weights [${weightLayers.size}]============\n" )
weightLayers.forEach { println ( "${it.size}::[${it.joinToString()}]" ) }
*/
	}

	override fun run ( ) {
		// split data set to training set and testing set
		val sampleSet : MutableList <Array <Any>> = dataSet.sampleSet
		//sampleSet.shuffle ( )
		val trainingCount = ( sampleSet.size.toDouble ( ) * .75 ).toInt ( )
		//val trainingSet = sampleSet.subList ( 0, trainingCount )
println ( "trainingset=${trainingCount} testingset=${sampleSet.size - trainingCount}" )
		//val testingSet = sampleSet.subList ( trainingCount, sampleSet.size )

		// create the set of derivatives in backpropagation
		val derivLayers = Array <DoubleArray> ( outputLayers.size ) {
			DoubleArray ( outputLayers [ it ].size )
		}

		var totalTrainingError = 0.0
		var totalValidatingError = 0.0
//sampleSet.forEach { println ( it.joinToString ( "-" ) ) }
		// foreach epoches
		for ( idx in 1 .. nEpoches ) {
			sampleSet.shuffle ( )

			// foeach sample
			//trainingSet.forEach { training ->
			sampleSet.forEachIndexed { idxSample, sample ->
				// foreach input to hidden layer
				var outputLayer = outputLayers [ 0 ]
				var weightLayer = weightLayers [ 0 ]
				var derivLayer = derivLayers [ 0 ]
				for ( idxOutput in 0 .. outputLayer.lastIndex ) {
					outputLayer [ idxOutput ] = 0.0
					for ( idxInput in 0 .. sample.size - 2 ) {
						outputLayer [ idxOutput ] += weightLayer [ idxOutput * sample.lastIndex + idxInput ] *
							( sample [ idxInput ] as Double ) // :TEMP: data is presumed to be Double
					} // foreach: input index

					// activate
					outputLayer [ idxOutput ] = activate ( outputLayer [ idxOutput ] )

					// d ( sigmoid ( neuron ) ) / d ( neuron )
					derivLayer [ idxOutput ] = outputLayer [ idxOutput ] * ( 1.0 - outputLayer [ idxOutput ] )
				} // foreach: output neuron index from input layer

				// foreach hidden layer to hidden/final layer
				for ( idxLayer in 1 .. outputLayers.lastIndex ) {
					val inputLayer = outputLayers [ idxLayer - 1 ]
					outputLayer = outputLayers [ idxLayer ]
					weightLayer = weightLayers [ idxLayer ]
					derivLayer = derivLayers [ idxLayer ]
					// foreach output
					for ( idxOutput in 0 .. outputLayer.lastIndex ) {
						// calculate the neuron
						outputLayer [ idxOutput ] = biases [ idxLayer - 1 ]

						for ( idxInput in 0 .. inputLayer.lastIndex ) {
							outputLayer [ idxOutput ] += weightLayer [ idxOutput * inputLayer.size + idxInput ] * inputLayer [ idxInput ]
						}

						// activate
						outputLayer [ idxOutput ] = activate ( outputLayer [ idxOutput ] )

						// d ( sigmoid ( neuron ) ) / d ( neuron )
						derivLayer [ idxOutput ] = outputLayer [ idxOutput ] * ( 1.0 - outputLayer [ idxOutput ] )
					} // foreach: output neuron index from hidden layer
				} // foreach: hidden-to-output layers

				// calculate the error
				outputLayer = outputLayers [ outputLayers.lastIndex ]
				derivLayer = derivLayers [ derivLayers.lastIndex ]
				var totalError = 0.0
				val correctOutputIndex = dataSet.labelNames.indexOf ( sample [ sample.lastIndex ] )	// find the index of correct output
//println ( dataSet.labelNames.joinToString ( " " ) )
//println ( "${sample[sample.lastIndex]}" )
//println ( sample.joinToString ( " " ) )
//println ( "correctidx=${correctOutputIndex}\tlabels.s=${dataSet.labelNames.size} O.s=${outputLayers.size}" )
				for ( idxOutput in 0 .. outputLayer.lastIndex ) {
					// calc the difference to correct value
					var error : Double = outputLayer [ idxOutput ]
					if ( idxOutput == correctOutputIndex ) {
						error -= 1.0 	// predicted - expected
					}

					// d(totalError)/d(output)
					derivLayer [ idxOutput ] *= error

					totalError += error.pow ( 2.0 ) / 2.0
//print ( "${outputLayers [ idxOutput ].joinToString ( )}\t" )
				}
				//totalError /= 2.0

				// backpropagation only for training set
				if ( idxSample < trainingCount ) {
					totalTrainingError += totalError
					//println ( "training error: $totalError" )	// collect the training result

					// backpropagation
					for ( idxLayer in outputLayers.lastIndex downTo 0 ) {
						// calc derivatives in output layer
						derivLayer = derivLayers [ idxLayer ]

						if ( idxLayer == outputLayers.lastIndex ) {
							// update bias
//println ( "\tb: ${biases [ biases.lastIndex ]}\t=> ${biases[biases.lastIndex] - learningRate * derivLayer.sum ( )}" )
							biases [ biases.lastIndex ] -= learningRate * derivLayer.sum ( )
						}

						if ( idxLayer > 0 ) {
							// derivative from hidden layer to output layer

							val inputLayer = outputLayers [ idxLayer - 1 ]
							var derivOutputSum = DoubleArray ( inputLayer.size )

							weightLayer = weightLayers [ idxLayer ]

							for ( idxOutput in 0 .. derivLayer.lastIndex ) {

								for ( idxInput in 0 .. inputLayer.lastIndex ) {
									// sum { d(totalError) / d(sigmoid(input)) }
									derivOutputSum [ idxInput ] += derivLayer [ idxOutput ] * weightLayer [ idxOutput * inputLayer.size + idxInput ]

									// update the weight
									// d(totalError) / d(inputWeight)
									weightLayer [ idxOutput * inputLayer.size + idxInput ] -= learningRate * derivLayer [ idxOutput ] * inputLayer [ idxInput ]
								}
//println ( "\td[$idxOutput]=${derivLayer[idxOutput]}" )
							}

							// d(toatlError)/d(input)
							val derivInputLayer = derivLayers [ idxLayer - 1 ]
							derivOutputSum.forEachIndexed { idxInput, deriv ->
								derivInputLayer [ idxInput ] *= deriv
							}

						} else if ( idxLayer > 0 ) {
							val inputLayer = outputLayers [ idxLayer - 1 ]
							var derivOutputSum = DoubleArray ( inputLayer.size )

							weightLayer = weightLayers [ idxLayer ]

							for ( idxInput in 0 .. inputLayer.lastIndex ) {

								for ( idxOutput in 0 .. derivLayer.lastIndex ) {
									// sum { d(totalError) / d(sigmoid(input)) }
									derivOutputSum [ idxInput ] += derivLayer [ idxOutput ] * weightLayer [ idxOutput * inputLayer.size + idxInput ]

									// update the weight
									// d(totalError) / d(inputWeight)
									weightLayer [ idxOutput * inputLayer.size + idxInput ] -= learningRate * derivLayer [ idxOutput ] * inputLayer [ idxInput ]
								}
							}

							// d(toatlError)/d(input)
							val derivInputLayer = derivLayers [ idxLayer - 1 ]
							derivOutputSum.forEachIndexed { idxInput, deriv ->
								derivInputLayer [ idxInput ] *= deriv
							}
						} else {
							// update weights in first layer

							weightLayer = weightLayers [ 0 ]

							for ( idxInput in 0 .. sample.size - 2 ) {
								for ( idxOutput in 0 .. derivLayer.lastIndex ) {
									weightLayer [ idxOutput * sample.lastIndex + idxInput ] -= learningRate * derivLayer [ idxOutput ] *
										( sample [ idxInput ] as Double ) // :TEMP: data is presumed to be Double
								}
							}
						}

					} // foreach: layers backword
//println ( "\tERROR\t=> ${totalError}" )
				} // if: backpropagate
				else {
					// record of the testing
					totalValidatingError += totalError
					errorSequence.add ( totalError )	// collect the training result
//println ( "\tERROR\t=> ${totalError}" )
				}

			} // foreach: training set
totalTrainingError /= trainingCount
totalValidatingError /= sampleSet.size - trainingCount
println ( "\ttrainingError=$totalTrainingError\ttestingError=$totalValidatingError" )
//println ( "=======================================================" )
// reset error count
totalTrainingError = 0.0
		} // foeach: epoches
	}

	fun activate ( value : Neuron ) : Neuron = 1.0 / ( 1.0 + E.pow ( - value ) )
}

class DataSet (
	val title : String,
	val labelNames : Array <String>,
	val features : ArrayList <Feature>,
	val sampleSet : MutableList <Array <Any>>
) {

	class Feature (
		val name : String,

		/**
		 * Feature type:
		 *	integer
		 *	numeric
		 *	real
		 *	string
		 *	{ string choice.. }
		 */
		val type : String
	) {
		init {
			//if ( type.startsWith ( "{" ) || type == "string" )
			//	throw IllegalArgumentException ( "Given feature type is not supported yet: ${type}" )
		}

		override fun toString ( ) : String {
			return "Feature [name=$name, type=$type]"
		}
	}
}

fun main ( argv : Array <String> ) {

	arrayOf <DataSet> (
			/*
		ArffParser.extractDataSet ( "diabetes", "C:\\Program Files\\Weka-3-8\\data\\diabetes.arff" ),
		ArffParser.extractDataSet ( "ionosphere", "C:\\Program Files\\Weka-3-8\\data\\ionosphere.arff" ),

		ArffParser.extractDataSet ( "segment-test", "C:\\Program Files\\Weka-3-8\\data\\segment-test.arff" ),
		ArffParser.extractDataSet ( "unbalanced", "C:\\Program Files\\Weka-3-8\\data\\unbalanced.arff" ),
		*/

		ArffParser.extractDataSet ( "iris", "C:\\Program Files\\Weka-3-8\\data\\iris.arff" )
		//ArffParser.extractDataSet ( "glass", "C:\\Program Files\\Weka-3-8\\data\\glass.arff" ),
		//ArffParser.extractDataSet ( "supermarket", "C:\\Program Files\\Weka-3-8\\data\\supermarket.arff" )
	).forEach {
		println ( " ======== [${it.title}] ============================================================" )
		Classifier ( 
			it,
			intArrayOf ( 6, 6 ),
			nEpoches = 50L,
			learningRate = .1
		).run ( )

		//flInputStream.close ( )
	}
}
