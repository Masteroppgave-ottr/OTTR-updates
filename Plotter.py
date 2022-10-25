
from ast import arg
import sys
import matplotlib.pyplot as plt


def find_all_solutions(measurement_list):
    solutions = []
    for i in measurement_list:
        if i[1] not in solutions:
            solutions.append(i[1])
    return solutions


def get_n(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(i[0])
    return matches


def get_time(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(i[3])
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


def create_bar_chart(measurement_list):
    solutions = find_all_solutions(measurement_list)

    baseline = None
    times = []
    for solution in solutions:
        n, time = get_n_and_time_lists(
            measurement_list, solution, "start", "end")

        print(solution, n, time)
        if solution == "rebuild set":
            baseline = int(time[0])
            print("baseline", baseline)
        else:
            times.append(int(time[0]))

        plt.bar(solution, time, label=solution)

    if baseline:
        # speedup with three decimal places
        speedup = baseline / times[0]
        speedup = int(speedup * 1000) / 1000
        plt.title(f"WOW {speedup} i speedup!")

    plt.show()


def create_line_graph(measurement_list):
    solutions = find_all_solutions(measurement_list)
    for solution in solutions:
        n, time = get_n_and_time_lists(
            measurement_list, solution, "start", "end")
        print(solution, n, time)
        plt.plot(n, time, label=solution)

    plt.xlabel("Number of triples")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title("Runtime")
    plt.show()


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
    create_bar_chart(measurement_list)


if __name__ == '__main__':
    # get first comand line argument
    all_measurements = read_file(sys.argv[1])

    default_plot(all_measurements)
