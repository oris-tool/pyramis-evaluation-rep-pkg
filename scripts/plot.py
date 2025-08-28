import os
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import argparse

def get_curve_label(filename):
    mapping = {
        'simulationCDF.txt': 'Simulation',
        'pyramisCDF.txt': 'Pyramis',
    }
    return mapping.get(filename, filename)

def get_curve_style(filename):
    if 'simulation' in filename:
        return {'color': '#EE7733', 'linewidth': 2}     # arancione acceso
    elif 'pyramis' in filename:
        return {'color': '#0077BB', 'linewidth': 2}     # blu intenso
    else:
        return {'color': '#333333', 'linewidth': 2}     # grigio scuro

def load_cdf_data(filepath):
    # File senza header: prima colonna x, seconda colonna y
    return pd.read_csv(filepath, delim_whitespace=True, header=None, names=['x', 'y'])

def plot_cdfs_from_folder(folder):
    filenames = ['simulationCDF.txt', 'pyramisCDF.txt']
    curves = []

    # plt.figure(figsize=(10, 6))
    plt.figure(figsize=(10, 4.5))

    plt.rcParams.update({'font.size': 22})  # Increases general font size
    plt.rc('axes', labelsize=22)    # Increases axes labels size
    plt.rc('legend', fontsize=20)   # Increases legend font size
    plt.rc('xtick', labelsize=22)   # Increases x-axis tick labels size
    plt.rc('ytick', labelsize=22)   # Increases y-axis tick labels size

    global_max_x = 0
    # Prima passata: trova il massimo valore di x globale
    for filename in filenames:
        file_path = os.path.join(folder, filename)
        if os.path.isfile(file_path):
            df = load_cdf_data(file_path)
            max_x = df['x'].max()
            global_max_x = max(global_max_x, max_x)

    # Seconda passata: plottaggio
    for filename in filenames:
        file_path = os.path.join(folder, filename)
        if os.path.isfile(file_path):
            df = load_cdf_data(file_path)
            style = get_curve_style(filename)
            curve = plt.plot(df['x'], df['y'],
                             label=get_curve_label(filename), 
                             **style)
            curves.append((curve[0], get_curve_label(filename)))

    plt.xlim(0, global_max_x * 1.05)
    plt.xlabel('time')
    plt.ylabel('cumulative distribution')
    plt.ylim(0, 1)
    handles, labels = zip(*curves)
    plt.legend(handles, labels, loc='lower right')
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(os.path.join(folder, "CDF_comparison.png"))
    plt.savefig(os.path.join(folder, "CDF_comparison.pdf"))
    plt.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Plot CDF from two files in the same folder.")
    parser.add_argument("folder", help="path to the folder containing simulation_CDF.txt and pyramis_CDF.txt")
    args = parser.parse_args()
    plot_cdfs_from_folder(args.folder)
