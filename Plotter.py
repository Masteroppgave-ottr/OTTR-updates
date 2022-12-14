import sys
import matplotlib.pyplot as plt
import numpy as np

# static variables
instances_index = 0
changes_idex = 1
solution_index = 2
tag_index = 3
time_index = 4


def find_all_solution_names(timestamp_list: list[list[str]]) -> list[str]:
    """
    Get the name of all the different solutions

    Returns:
        list[str]: all solution names in a list
    """
    solutions = []
    for i in timestamp_list:
        if i[solution_index] not in solutions:
            solutions.append(i[solution_index])
    return solutions


def find_all_tag_names(timestamp_list: list[list[str]]) -> list[str]:
    """
    Get the name of all the different tags

    Returns:
        list[str]: all tag names in a list
    """
    tags = []
    for i in timestamp_list:
        if i[tag_index] not in tags:
            tags.append(i[tag_index])
    return tags


def get_instances(timestamp_list: list[list[str]]) -> list[int]:
    """
    get a list of all the instances for the given solution
    """
    matches = []
    for i in timestamp_list:
        matches.append(int(i[instances_index]))
    return matches


def get_changes(timestamp_list: list[list[str]]) -> list[int]:
    """
    get a list of all the changes for the given solution
    """
    matches = []
    for i in timestamp_list:
        matches.append(int(i[changes_idex]))
    return matches


def get_time(timestamp_list: list[list[str]]) -> list[int]:
    """
    get a list of all the timestamps for the given solution
    """
    matches = []
    for i in timestamp_list:
        matches.append(int(i[time_index]))
    return matches


def find_interval(timestamp_list: list[list[str]], solution: str, start_tag: str, end_tag: str, field=instances_index) -> list[list[str]]:
    """
    For a given solution, find every timestamp in the interval between `start_tag` and `end_tag` 

    Returns:
        list[list[str]]: list of timestamps in the interval
    """
    matches = []
    for i in timestamp_list:
        if i[solution_index] == solution and i[tag_index] == start_tag:
            # find the next measurement with the same solution and end
            for j in timestamp_list:
                if j[field] == i[field] and j[solution_index] == solution and j[tag_index] == end_tag:
                    matches.append([i[instances_index], i[changes_idex], i[solution_index],
                                   j[tag_index], int(j[time_index]) - int(i[time_index])])
                    break

    return matches


def get_instance_change_time_lists(timestamp_list: list[list[str]], solution_name: str, start_tag: str, end_tag: str, field=instances_index) -> tuple[list[int], list[int], list[int]]:
    """find n and time for measurements for the given solution between `start` and `end`
    """
    interval = find_interval(
        timestamp_list, solution_name, start_tag, end_tag, field)
    n = get_instances(interval)
    changes = get_changes(interval)
    time = get_time(interval)
    return n, changes, time


def read_file(filename: str) -> list[list[str]]:
    """
    read the content of the file, and parse to list
    Format of file content per line:
        ``` instances ; changes ; solution ; label ; time ```
    Example:
        ```10 ; 2 ; naive solution ; start ; 34328947247251```
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


def create_bar_interval(timestamp_list: list[list[str]], labels: list[str] = ["diff", "model"], plot_labels: list[str] = ["diff", "expand instances", "query"]) -> None:
    """
    Create a bar chart for the given solution, between the given labels
    Args:
        timestamp_list: list of timestamps
        labels: labels we want to measure between. Start and end is added automatically
        plot_labels: labels for the bars on the x-axis
    """
    # error handling
    if len(labels) + 1 != len(plot_labels):
        raise Exception(
            "plot lables need to have one more element than labels")

    solutions = find_all_solution_names(timestamp_list)
    width = 0.2
    x = np.arange(len(labels)+1)
    labels = ["start"] + labels + ["end"]
    counter = 0
    instances = [-1]
    changes = [-1]
    for solution in solutions:
        # skip the rebuild set solution
        if (solution == "rebuild set"):
            continue
        counter += 1

        # find the time for each interval
        solutionTimes = []
        for i in range(1, len(labels)):
            instances, changes, time = get_instance_change_time_lists(
                timestamp_list, solution, labels[i-1], labels[i])
            if len(time) == 0:
                raise Exception("ERROR: Cant find any time in the interval:",
                                labels[i-1], labels[i])
            else:
                solutionTimes.append(time[0])

        # create bar for the solution
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


def create_line_graph_nInstances(timestamp_list: list[list[str]]) -> None:
    """
    Create a line graph for the given solution, The time is between the start and end tag.
    """
    solutions = find_all_solution_names(timestamp_list)
    for solution in solutions:
        n, changes, time = get_instance_change_time_lists(
            timestamp_list, solution, "start", "end", instances_index)

        plt.plot(n, time, label=solution)

    plt.xlabel("number of instances")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(
        f"Runtime | Number of changes = {changes[0]}")
    print("[PLOT] Creating line graph")
    plt.savefig("./temp/line.png")


def create_line_graph_nChanges(timestamp_list: list[list[str]]) -> None:
    """
    Create a line graph for the given solution, The time is between the start and end tag.
    """
    solutions = find_all_solution_names(timestamp_list)
    for solution in solutions:
        instances, changes, time = get_instance_change_time_lists(
            timestamp_list, solution, "start", "end", changes_idex)

        plt.plot(changes, time, label=solution)

    plt.xlabel("number of changes")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(
        f"Runtime | Number of Instances = {instances[0]}")
    print("[PLOT] Creating line graph")
    plt.savefig("./temp/line.png")


def has_multiple_n(timestamp_list: list[list[str]], field=instances_index) -> bool:
    """
    check if there more than one n for all timestamps

    Args:
        timestamp_lists: list of timestamps
        field: the field containing the n. Default is number of instances
    """
    first_n = timestamp_list[0][field]
    for measurement in timestamp_list:
        if (measurement[field] != first_n):
            return True
    return False


if __name__ == '__main__':
    plot_type = sys.argv[1]
    timestamp_list = read_file(sys.argv[2])

    if plot_type == "n=instances":
        if has_multiple_n(timestamp_list, instances_index):
            create_line_graph_nInstances(timestamp_list)
        else:
            create_bar_interval(timestamp_list,
                                ["diff", "model"],
                                ["diff", "expand instances", "query"])

    elif plot_type == "n=changes":
        if has_multiple_n(timestamp_list, changes_idex):
            create_line_graph_nChanges(timestamp_list)
        else:
            create_bar_interval(timestamp_list,
                                ["diff", "model"],
                                ["diff", "expand instances", "query"])

    else:
        print(f"Unknown plot type '{plot_type}'")
        sys.exit(1)
