
from ast import arg
from cProfile import label
import sys
import matplotlib.pyplot as plt
import numpy as np

# static variables
instances_index = 0
changes_idex = 1
solution_index = 2
tag_index = 3
time_index = 4


def find_all_solutions(measurement_list):
    solutions = []
    for i in measurement_list:
        if i[solution_index] not in solutions:
            solutions.append(i[solution_index])
    return solutions


def get_instances(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[instances_index]))
    return matches


def get_changes(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[changes_idex]))
    return matches


def get_time(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[time_index]))
    return matches


def find_interval(measurement_list, solution, start_tag, end_tag, field=instances_index):
    """
    For a given solution, find the interval between `start` and `end` for every n
    """
    matches = []
    for i in measurement_list:
        if i[solution_index] == solution and i[tag_index] == start_tag:
            # find the next measurement with the same solution and end
            for j in measurement_list:
                if j[field] == i[field] and j[solution_index] == solution and j[tag_index] == end_tag:
                    matches.append([i[instances_index], i[changes_idex], i[solution_index],
                                   j[tag_index], int(j[time_index]) - int(i[time_index])])
                    break

    return matches


def get_instance_change_time_lists(measurement_list, solution, start, end, field=instances_index):
    """find n and time for measurements for the given solution between `start` and `end`
    """
    interval = find_interval(measurement_list, solution, start, end, field)
    n = get_instances(interval)
    changes = get_changes(interval)
    time = get_time(interval)
    return n, changes, time


def read_file(filename):
    """
    read the content of the file, and parse to list
    Format of file content per line: ``` instances ; changes ; solution ; label ; time ```
    Example: ```10 ; 2 ; naive solution ; start ; 34328947247251```
    """
    measurements = []
    with open(filename, "r") as f:
        lines = f.readlines()

        for line in lines:
            if len(line) == 1:
                continue
            line = line.strip()
            args = line.split(" ; ")
            measurements.append(args)
    return measurements


def create_bar_interval(measurement_list, field=instances_index, labels=["diff", "model", "pikk"], plot_labels=["diff", "expand instances", "pikken", "query"]):
    """
    Create a bar chart for the given solution, between the given labels
    Input:
        measurement_list: list of measurements
        field: the field to use for the x-axis
        labels: labels we want to measure between
        plot_labels: labels to use for the x-axis
    """
    # error handling
    if len(labels) + 1 != len(plot_labels):
        raise Exception(
            "plot lables need to have one more element than labels")

    solutions = find_all_solutions(measurement_list)
    width = 0.2
    x = np.arange(len(labels)+1)
    counter = 0
    instances = [-1]
    changes = [-1]
    labels = ["start"] + labels + ["end"]
    for solution in solutions:
        if (solution == "rebuild set"):
            continue
        counter += 1

        solutionTimes = []
        for i in range(1, len(labels)):
            instances, changes, time = get_instance_change_time_lists(
                measurement_list, solution, labels[i-1], labels[i], field)
            if len(time) == 0:
                raise Exception("ERROR: Cant find any time in the interval:",
                                labels[i-1], labels[i])
            else:
                solutionTimes.append(time[0])

        if (len(solutions) == 1):
            plt.bar(x, solutionTimes, width=width, label=solution)
        elif (counter % 2 == 0):
            plt.bar(x+((width/2)*(counter-1)),
                    solutionTimes, width=width, label=solution)
        else:
            plt.bar(x-((width/2)*counter),
                    solutionTimes, width=width, label=solution)

    plt.xticks(x, plot_labels)
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(f"Instances = {instances[0]} | Changes = {changes[0]}")
    print("[PLOT] Creating bar chart")
    plt.savefig("./temp/bar.png")


def create_line_graph_nInstances(measurement_list):
    solutions = find_all_solutions(measurement_list)
    for solution in solutions:
        n, changes, time = get_instance_change_time_lists(
            measurement_list, solution, "start", "end", instances_index)

        plt.plot(n, time, label=solution)

    plt.xlabel("number of instances")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(
        f"Runtime | Number of changes = {changes[0]}")
    print("[PLOT] Creating line graph")
    plt.savefig("./temp/line.png")


def create_line_graph_nChanges(measurement_list):
    solutions = find_all_solutions(measurement_list)
    for solution in solutions:
        instances, changes, time = get_instance_change_time_lists(
            measurement_list, solution, "start", "end", changes_idex)

        plt.plot(changes, time, label=solution)

    plt.xlabel("number of changes")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(
        f"Runtime | Number of Instances = {instances[0]}")
    print("[PLOT] Creating line graph")
    plt.savefig("./temp/line.png")


def has_multiple_n(measurement_list, field=instances_index):
    print(measurement_list)
    first_n = measurement_list[0][field]
    for measurement in measurement_list:
        if (measurement[field] != first_n):
            return True
    return False


if __name__ == '__main__':
    # get first command line argument
    print(sys.argv)
    plot_type = sys.argv[1]
    all_measurements = read_file(sys.argv[2])

    if plot_type == "n=instances":
        if has_multiple_n(all_measurements, instances_index):
            create_line_graph_nInstances(all_measurements)
        else:
            create_bar_interval(all_measurements, instances_index)

    elif plot_type == "n=changes":
        if has_multiple_n(all_measurements, changes_idex):
            create_line_graph_nChanges(all_measurements)
        else:
            create_bar_interval(all_measurements, changes_idex)

    else:
        print(f"Unknown plot type '{plot_type}'")
        sys.exit(1)
