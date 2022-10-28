
from ast import arg
from cProfile import label
import sys
import matplotlib.pyplot as plt
import numpy as np


def find_all_solutions(measurement_list):
    solutions = []
    for i in measurement_list:
        if i[1] not in solutions:
            solutions.append(i[1])
    return solutions


def get_n(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[0]))
    return matches


def get_time(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[3]))
    return matches


def find_interval(measurement_list, solution, start_tag, end_tag):
    """
    For a given solution, find the interval between `start` and `end` for every n
    """
    matches = []
    for i in measurement_list:
        if i[1] == solution and i[2] == start_tag:
            # find the next measurement with the same solution and end
            for j in measurement_list:
                if j[0] == i[0] and j[1] == solution and j[2] == end_tag:
                    matches.append([i[0], i[1], j[2], int(j[3]) - int(i[3])])
                    break

    return matches


def get_n_and_time_lists(measurement_list, solution, start, end):
    """find n and time for measurements for the given solution between `start` and `end`
    """
    interval = find_interval(measurement_list, solution, start, end)
    n = get_n(interval)
    time = get_time(interval)
    return n, time


def read_file(filename):
    """
    read the content of the file, and parse to list
    Format of file content: ``` n, solution, label, time ```
    Example: ```10 ; naive solution ; start ; 34328947247251```
    """
    measurements = []
    with open(filename, "r") as f:
        lines = f.readlines()

        for line in lines:
            line = line.strip()
            args = line.split(" ; ")
            measurements.append(args)
    return measurements


def create_bar_interval(measurement_list):
    solutions = find_all_solutions(measurement_list)
    width = 0.2
    x = np.arange(3)
    counter = 0
    for solution in solutions:
        counter += 1
        solutionTimes = []
        n, diffTime = get_n_and_time_lists(
            measurement_list, solution, "start", "diff")
        n, modelTime = get_n_and_time_lists(
            measurement_list, solution, "diff", "model")
        n, queryTime = get_n_and_time_lists(
            measurement_list, solution, "model", "end")
        
        solutionTimes.append(diffTime[0])
        solutionTimes.append(modelTime[0])
        solutionTimes.append(queryTime[0])
        y1 = [34, 56, 12, 89, 67]

        if (len(solutions) == 1):
            plt.bar(x, solutionTimes, width=width, label=solution)
        elif (counter % 2 == 0):
            plt.bar(x+((width/2)*(counter-1)), solutionTimes, width, label=solution)
        else: 
            plt.bar(x-((width/2)*counter), solutionTimes, width, label=solution)


    plt.xticks(x, ["diff", "model", "query"])
    plt.xlabel("Sections")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.show()
    plt.savefig("./temp/bar_sections.png")

def create_bar_chart(measurement_list):
    solutions = find_all_solutions(measurement_list)

    baseline = None
    times = []
    for solution in solutions:
        n, time = get_n_and_time_lists(
            measurement_list, solution, "start", "end")

        if solution == "rebuild set":
            baseline = int(time[0])
        else:
            times.append(int(time[0]))

        plt.bar(solution, time, label=solution)

    if baseline:
        # speedup with three decimal places
        speedup = baseline / times[0]
        speedup = int(speedup * 1000) / 1000
        plt.title(f"WOW {speedup} i speedup!")

    plt.savefig("./temp/bar.png")


def create_line_graph(measurement_list):
    solutions = find_all_solutions(measurement_list)
    for solution in solutions:
        n, time = get_n_and_time_lists(
            measurement_list, solution, "start", "end")
        plt.plot(n, time, label=solution)

    plt.xlabel("Number of triples")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title("Runtime")
    plt.savefig("./temp/line.png")


def default_plot(measurement_list):
    """
    try to plot the data. Each solution is a separate line.
    requires the label to include "start" and "end"
    """

    first_n = measurement_list[0][0]
    for measurement in measurement_list:
        if (measurement[0] != first_n):
            create_line_graph(measurement_list)
            return
    create_bar_interval(measurement_list)


if __name__ == '__main__':
    # get first comand line argument
    all_measurements = read_file(sys.argv[1])

    default_plot(all_measurements)
