
/**
 * @author BingLi224
 * @version	2019.02.03
 * 
 * Nueral Network : DataSet
 *
 * Both features and label in one row.
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

typealias Neuron = Double
typealias NeuronArray = DoubleArray
typealias Weight = Double
typealias WeightArray = DoubleArray
typealias Bias = Double
typealias BiasArray = DoubleArray
typealias Label = Double
typealias LabelArray = DoubleArray
//typealias Array <Label> = DoubleArray

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
