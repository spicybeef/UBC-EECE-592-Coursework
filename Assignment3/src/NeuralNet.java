import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.util.Random;

/**
 * NeuralNet class for Assignment 1a of EECE592. A simple neural network with one hidden layer,
 * a selectable number of hidden neurons (up to MAX_HIDDEN_NEURONS) and a selectable number of
 * inputs (up to MAX_INPUTS)
 */
class NeuralNet
{
    // Constants
    private static final int MAX_HIDDEN_NEURONS = 16;
    private static final int MAX_INPUTS =         16;
    private static final int MAX_OUTPUTS =        16;

    // Private member variables
    // Limits for custom sigmoid activation function used by the output neuron
    private double mArgA;
    private double mArgB;
    private double mWeightInitMin;
    private double mWeightInitMax;

    // Public member variables
    // Neural network parameters
    // We only have a single hidden layer with a provided number of inputs and hidden neurons
    private int mNumInputs;
    private int mNumOutputs;
    private int mNumHiddenNeurons;

    // Learning rate and momentum term
    private double mLearningRate;
    private double mMomentumTerm;

    // Array to store input values to the neural network, first index is bias input of 1.0
    private double[] mInputValues = new double[MAX_INPUTS];

    // Array to store input weights to the neurons of the hidden layer
    private static double[][] mInputWeights = new double[MAX_HIDDEN_NEURONS][MAX_INPUTS];
    // Array to store previous weights
    private static double[][] mPreviousInputWeights = new double[MAX_HIDDEN_NEURONS][MAX_INPUTS];

    // Array to store unactivated neuron outputs of the hidden layer
    private double[] mHiddenNeuronUnactivatedOutputs = new double[MAX_HIDDEN_NEURONS];
    // Array to store neuron outputs of the hidden layer
    private double[] mHiddenNeuronOutputs = new double[MAX_HIDDEN_NEURONS];
    // Array to store neuron errors of the hidden layer
    private double[] mHiddenNeuronErrors = new double[MAX_HIDDEN_NEURONS];

    // Array to store the output neuron's input weights
    private static double[][] mOutputNeuronWeights = new double[MAX_OUTPUTS][MAX_HIDDEN_NEURONS];
    // Array to store the previous output neuron's weights
    private static double[][] mPreviousOutputNeuronWeights = new double[MAX_OUTPUTS][MAX_HIDDEN_NEURONS];
    // Variables for output neuron bias weight
    private static double[] mOutputNeuronBiasWeights = new double[MAX_OUTPUTS];
    private static double[] mPreviousOutputNeuronBiasWeights = new double[MAX_OUTPUTS];

    // Variable for unactivated output neuron value
    private double[] mOutputNeuronUnactivatedValues = new double[MAX_OUTPUTS];
    // Variable for value of output neuron
    private double[] mOutputNeuronValues = new double[MAX_OUTPUTS];
    // Variable for out neuron error
    private double[] mOutputNeuronErrors = new double[MAX_OUTPUTS];

    /**
     * Constructor for NeuralNet
     * @param argNumInputs The number of inputs in your input vector
     * @param argNumHidden The number of hidden neurons in your hidden layer. Only a single hidden layer is supported
     * @param argLearningRate The learning rate coefficient
     * @param argMomentumTerm The momentum coefficient
     * @param argA Integer lower bound of sigmoid used by the output neuron only.
     * @param argB Integer upper bound of sigmoid used by the output neuron only.
    */
    public NeuralNet(int argNumInputs,
                     int argNumOutputs,
                     int argNumHidden,
                     double argLearningRate,
                     double argMomentumTerm,
                     double argA,
                     double argB,
                     double argWeightInitMin,
                     double argWeightInitMax)
    {
        // Update our private variables
        mArgA = argA;
        mArgB = argB;
        mWeightInitMin = argWeightInitMin;
        mWeightInitMax = argWeightInitMax;
        // Add one here so that we don't worry about it later in the code (for bias)
        mNumInputs = argNumInputs + 1;
        mNumOutputs = argNumOutputs;
        mNumHiddenNeurons = argNumHidden;
        // Record the learning and momentum rates
        mLearningRate = argLearningRate;
        mMomentumTerm = argMomentumTerm;
        // Zero out the weights (also clears previous entry)
        zeroWeights();

        // System.out.format("Hi. Neural net instantiated with %5d inputs and %5d hidden neurons.\n", mNumInputs-1, mNumHiddenNeurons-1);
    }

    /**
     * This method implements the sigmoid function
     * @param x The input
     * @return f(x) = 1 / (1 + exp(-x))
     */
    private double sigmoid(double x)
    {
        double result;

        result =  1 / (1 + Math.exp(-x));

        return result;
    }

    /**
     * This method implements the first derivative of the sigmoid function
     * @param x The input
     * @return f'(x) = (1 / (1 + exp(-x)))(1 - (1 / (1 + exp(-x))))
     */
    public double sigmoidDerivative(double x)
    {
        double result;

        result = sigmoid(x)*(1 - sigmoid(x));

        return result;
    }

    /**
     * This method implements a general sigmoid with asymptotes bounded by (a,b)
     * @param x The input
     * @return f(x) = (b - a) / (1 + exp(-x)) + a
     */
    public double customSigmoid(double x)
    {
        double result;

        result = (mArgB - mArgA) * sigmoid(x) + mArgA;

        return result;
    }

    /**
     * This method implements the first derivative of the general sigmoid above
     * @param x The input
     * @return f'(x) = (1 / (b - a))(customSigmoid(x) - a)(b - customSigmoid(x))
     */
    public double customSigmoidDerivative(double x)
    {
        double result;

        result = (1.0/(mArgB - mArgA)) * (customSigmoid(x) - mArgA) * (mArgB - customSigmoid(x));

        return result;
    }

    /**
     * Initialize the weights to a random value between WEIGHT_INIT_MIN and WEIGHT_INIT_MAX
     */
    public void initializeWeights()
    {
        int i, j;

        // initialize inner neuron weights
        for(i = 0; i < mNumHiddenNeurons; i++)
        {
            for(j = 0; j < mNumInputs; j++)
            {
                mInputWeights[i][j] = getRandomDouble(mWeightInitMin, mWeightInitMax);
            }
        }

        // initialize outer neuron weights
        for(i = 0; i < mNumOutputs; i++)
        {
            for(j = 0; j < mNumHiddenNeurons; j++)
            {
                // initialize the output neuron weights
                mOutputNeuronWeights[i][j] = getRandomDouble(mWeightInitMin, mWeightInitMax);
                mOutputNeuronBiasWeights[i] = getRandomDouble(mWeightInitMin, mWeightInitMax);
            }
        }

        // Copy the initial weights into the delta tracking variables
        mPreviousInputWeights = mInputWeights.clone();
        mPreviousOutputNeuronWeights = mOutputNeuronWeights.clone();
        mPreviousOutputNeuronBiasWeights = mOutputNeuronBiasWeights.clone();
    }

    private double calculateWeightDelta(double weightInput, double error, double currentWeight, double previousWeight)
    {
        double momentumTerm, learningTerm;

        momentumTerm = mMomentumTerm * (currentWeight - previousWeight);
        learningTerm = mLearningRate * error * weightInput;
        return (momentumTerm + learningTerm);
    }

    /**
     * Updates the weights based on the current backpropagated error
     */
    private void updateWeights()
    {
        int hiddenNeuron, outputNeuron, input;
        double[] newOutputNeuronBiasWeights = new double[MAX_OUTPUTS];
        double[][] newOutputNeuronWeights = new double[MAX_HIDDEN_NEURONS][MAX_OUTPUTS];
        double[][] newInputNeuronWeights = new double[MAX_HIDDEN_NEURONS][MAX_INPUTS];

        // Update the weights from hidden neurons to the output neurons
        // Update the weight from bias input to the output neurons
        for(outputNeuron = 0; outputNeuron < mNumOutputs; outputNeuron++)
        {
            // Bias input weight
            newOutputNeuronBiasWeights[outputNeuron] =
                mOutputNeuronBiasWeights[outputNeuron] +
                calculateWeightDelta(1.0,
                    mOutputNeuronErrors[outputNeuron],
                    mOutputNeuronBiasWeights[outputNeuron],
                    mPreviousOutputNeuronBiasWeights[outputNeuron]);

            // Hidden neuron weights
            for(hiddenNeuron = 0; hiddenNeuron < mNumHiddenNeurons; hiddenNeuron++)
            {
                newOutputNeuronWeights[outputNeuron][hiddenNeuron] =
                    mOutputNeuronWeights[outputNeuron][hiddenNeuron] +
                    calculateWeightDelta(
                        mHiddenNeuronOutputs[hiddenNeuron],
                        mOutputNeuronErrors[outputNeuron],
                        mOutputNeuronWeights[outputNeuron][hiddenNeuron],
                        mPreviousOutputNeuronWeights[outputNeuron][hiddenNeuron]);
            }
        }

        // Update the weights to the hidden neurons
        for(hiddenNeuron = 0; hiddenNeuron < mNumHiddenNeurons; hiddenNeuron++)
        {
            for(input = 0; input < mNumInputs; input++)
            {
                newInputNeuronWeights[hiddenNeuron][input] = mInputWeights[hiddenNeuron][input] +
                    calculateWeightDelta(
                        mInputValues[input],
                        mHiddenNeuronErrors[hiddenNeuron],
                        mInputWeights[hiddenNeuron][input],
                        mPreviousInputWeights[hiddenNeuron][input]);
            }
        }

        mPreviousOutputNeuronBiasWeights = mOutputNeuronBiasWeights;
        mPreviousOutputNeuronWeights = mOutputNeuronWeights.clone();
        mPreviousInputWeights = mInputWeights.clone();

        mOutputNeuronBiasWeights = newOutputNeuronBiasWeights;
        mOutputNeuronWeights = newOutputNeuronWeights.clone();
        mInputWeights = newInputNeuronWeights.clone();
    }

    public void printNeuronOutputs()
    {
        int i;

        System.out.println("Current neuron outputs are as follows:");
        System.out.println("\tHidden neuron outputs");
        for (i = 0; i < mNumHiddenNeurons; i++)
        {
            System.out.format("%d, %5f\n", i, mHiddenNeuronOutputs[i]);
        }
        System.out.println("\tOutput neuron outputs");
        for (i = 0; i < mNumOutputs; i++)
        {
            System.out.format("%d, %5f\n", i, mOutputNeuronValues[i]);
        }
    }

    public void printNeuronErrors()
    {
        int i;

        System.out.println("Current neuron errors are as follows:");
        System.out.println("\tHidden neuron errors");
        for(i = 0; i < mNumHiddenNeurons; i++)
        {
            System.out.format("%d, %5f\n", i, mHiddenNeuronErrors[i]);
        }
        System.out.println("\tOutput neuron errors");
        for (i = 0; i < mNumOutputs; i++)
        {
            System.out.format("%d, %5f\n", i, mOutputNeuronErrors[i]);
        }
    }

    public void printWeights()
    {
        int i, j;

        System.out.println("Current neuron weights are as follows:");
        System.out.println("\tHidden neuron weights");
        for(i = 0; i < mNumHiddenNeurons; i++)
        {
            for (j = 0; j < mNumInputs; j++)
            {
                System.out.format("%d, %d, %5f\n", i, j, mInputWeights[i][j]);
            }
        }
        System.out.println("\tOutput neuron weights");
        for(i = 0; i < mNumOutputs; i++)
        {
            System.out.format("\t\t%d", i);
            System.out.format("b, %5f\n", mOutputNeuronBiasWeights[i]);
            for (j = 0; j < mNumHiddenNeurons; i++)
            {
                System.out.format("%d, %5f\n", i, mOutputNeuronWeights[i][j]);
            }
        }
    }

    /**
     * Returns a random double value between specified min and max values
     * @param min minimum number random number can be
     * @param max maximum number random number can be
     * @return a random double between specified min and max
     */
    private double getRandomDouble(double min, double max)
    {
        double random, result;

        random = new Random().nextDouble();
        result = min + (random * (max - min));

        return result;
    }

    private void zeroWeights()
    {
        int i, j;

        // initialize inner neurons weights
        for(i = 0; i < mNumHiddenNeurons; i++)
        {
            for(j = 0; j < mNumInputs; j++)
            {
                mInputWeights[i][j] = 0.0;
                mPreviousInputWeights[i][j] = 0.0;
            }
        }

        // initialize output neuron weights
        for(i = 0; i < mNumOutputs; i++)
        {
            for(j = 0; j < mNumHiddenNeurons; j++)
            {
                mPreviousOutputNeuronWeights[i][j] = 0.0;
                mOutputNeuronWeights[i][j] = 0.0;
            }
        }
    }

    /**
     * @param x The input vector. An array of doubles.
     * @return The value returned by the NN for this input vector
     */
    private double[] outputFor(double[] x)
    {
        int hiddenNeuron, outputNeuron, input;

        mInputValues = x;

        // Calculate hidden neuron outputs
        // Bias is included in input vector as the first index
        for(hiddenNeuron = 0; hiddenNeuron < mNumHiddenNeurons; hiddenNeuron++)
        {
            mHiddenNeuronUnactivatedOutputs[hiddenNeuron] = 0.0;
            // iterate over bias input + inputs
            for(input = 0; input < mNumInputs; input++)
            {
                mHiddenNeuronUnactivatedOutputs[hiddenNeuron] += mInputWeights[hiddenNeuron][input] * mInputValues[input];
            }
            // Apply the activation function to the weighted sum
            mHiddenNeuronOutputs[hiddenNeuron] = customSigmoid(mHiddenNeuronUnactivatedOutputs[hiddenNeuron]);
        }

        // Calculate the output of the output neurons
        for(outputNeuron = 0; outputNeuron < mNumOutputs; outputNeuron++)
        {
            mOutputNeuronUnactivatedValues[outputNeuron] = 0.0;
            for(hiddenNeuron = 0; hiddenNeuron < mNumHiddenNeurons; hiddenNeuron++)
            {
                mOutputNeuronUnactivatedValues[hiddenNeuron] += mHiddenNeuronOutputs[hiddenNeuron] * mOutputNeuronWeights[outputNeuron][hiddenNeuron];
            }
            // Add the output bias
            mOutputNeuronUnactivatedValues[outputNeuron] += (1.0 * mOutputNeuronBiasWeights[outputNeuron]);
            // Apply the activation function to the weighted sum
            mOutputNeuronValues[outputNeuron] = customSigmoid(mOutputNeuronUnactivatedValues[outputNeuron]);
        }

        return mOutputNeuronValues;
    }

    /**
     * This method calculates the error based on the current input & output.
     * It is expected that outputFor has been called before this method call.
     * @param expectedValues The expected output value for the current input
     */
    private void calculateErrors(double[] expectedValues)
    {
        int hiddenNeuron, outputNeuron;
        double summedWeightedErrors;

        for(outputNeuron = 0; outputNeuron < mNumOutputs; outputNeuron++)
        {
            // Calculate the output error from the feed forward
            mOutputNeuronErrors[outputNeuron] = (expectedValues[outputNeuron] - mOutputNeuronValues[outputNeuron]) * customSigmoidDerivative(mOutputNeuronUnactivatedValues[outputNeuron]);

            // Backpropagate the output error
            for(hiddenNeuron = 0; hiddenNeuron < mNumHiddenNeurons; hiddenNeuron++)
            {
                summedWeightedErrors = 0.0;

                // Sum all of the output neuron errors * hidden neuron weights
                for(outputNeuron = 0; outputNeuron < mNumOutputs; outputNeuron++)
                {

                }
                mHiddenNeuronErrors[hiddenNeuron] = mOutputNeuronErrors[outputNeuron] *
                    mOutputNeuronWeights[outputNeuron][hiddenNeuron] *
                    customSigmoidDerivative(mHiddenNeuronUnactivatedOutputs[hiddenNeuron]);
            }
        }
    }

    /**
     * This method will tell the NN the output
     * value that should be mapped to the given input vector. I.e.
     * the desired correct output value for an input.
     * @param x The input vector
     * @param argValue The new value to learn
     * @return The error in the output for that input vector
     */
    public double[] train(double[] x, double[] argValue)
    {
        int i;
        double[] errors = new double[mNumOutputs];

        // Feed forward stage: calculate the output value
        // this will update the neuron outputs
        outputFor(x);

        // Calculate errors
        calculateErrors(argValue);

        // perform weight update
        updateWeights();

        for(i = 0; i < mNumOutputs; i++)
        {
            errors[i] = argValue[i] - mOutputNeuronValues[i];
        }

        // Return the errors in the outputs from what we expected
        return errors;
    }

    public void save(File argFile)
    {

    }

    public void load(String argFileName) throws IOException
    {

    }
}