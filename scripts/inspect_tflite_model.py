import sys

import tensorflow as tf


def main() -> None:
    model_path = sys.argv[1]
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    print("inputs")
    for detail in interpreter.get_input_details():
        print(detail)

    print("outputs")
    for detail in interpreter.get_output_details():
        print(detail)


if __name__ == "__main__":
    main()
