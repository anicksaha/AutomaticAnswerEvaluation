from pprint import pprint

from entity_linker import POSTracker


class EvaluateAnswer:

    def __init__(self):
        self.pos_tracker = POSTracker()

    def test(self):
        word = "Anil Played Football"
        self.pos_tracker.get_pos(word)

        word = "Football is played by Anil"
        # self.pos_tracker.get_pos(word)

if __name__ == "__main__":
    evaluate_answer = EvaluateAnswer()
    evaluate_answer.test()